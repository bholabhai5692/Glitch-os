#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int photorec_cli_main(int argc, char **argv);
extern int need_to_stop;

static pthread_mutex_t scan_mutex = PTHREAD_MUTEX_INITIALIZER;
static int scan_active = 0;

JNIEXPORT jint JNICALL
Java_com_example_tools_recover_PhotoRecNative_runScan(
    JNIEnv *env,
    jobject receiver,
    jint source_fd,
    jstring output_directory,
    jstring progress_log) {
  (void)receiver;
  if (pthread_mutex_trylock(&scan_mutex) != 0) {
    return -4;
  }

  jint result = -1;
  const char *output_path = (*env)->GetStringUTFChars(env, output_directory, NULL);
  if (output_path == NULL) {
    goto cleanup;
  }
  const char *progress_path = (*env)->GetStringUTFChars(env, progress_log, NULL);
  if (progress_path == NULL) {
    (*env)->ReleaseStringUTFChars(env, output_directory, output_path);
    goto cleanup;
  }

  struct stat source_info;
  struct stat output_info;
  if (fstat(source_fd, &source_info) != 0 || lseek(source_fd, 0, SEEK_CUR) < 0 ||
      !S_ISREG(source_info.st_mode) || source_info.st_size <= 0 ||
      stat(output_path, &output_info) != 0 || !S_ISDIR(output_info.st_mode) ||
      access(output_path, W_OK) != 0 || progress_path[0] == '\0') {
    result = -2;
    goto release_paths;
  }

  char source_path[64];
  const int source_path_length =
      snprintf(source_path, sizeof(source_path), "/proc/self/fd/%d", source_fd);
  if (source_path_length < 0 || (size_t)source_path_length >= sizeof(source_path)) {
    result = -3;
    goto release_paths;
  }

  char *arguments[] = {
      "photorec",
      "/nolog",
      "/logjson",
      (char *)progress_path,
      "/d",
      (char *)output_path,
      "/cmd",
      source_path,
      "fileopt,everything,enable,search",
      NULL};
  need_to_stop = 0;
  __atomic_store_n(&scan_active, 1, __ATOMIC_RELEASE);
  result = photorec_cli_main(9, arguments);
  __atomic_store_n(&scan_active, 0, __ATOMIC_RELEASE);
  if (need_to_stop != 0) {
    result = -5;
  }
  need_to_stop = 0;

release_paths:
  (*env)->ReleaseStringUTFChars(env, progress_log, progress_path);
  (*env)->ReleaseStringUTFChars(env, output_directory, output_path);
cleanup:
  pthread_mutex_unlock(&scan_mutex);
  return result;
}

JNIEXPORT void JNICALL
Java_com_example_tools_recover_PhotoRecNative_cancelScan(JNIEnv *env, jobject receiver) {
  (void)env;
  (void)receiver;
  if (__atomic_load_n(&scan_active, __ATOMIC_ACQUIRE) != 0) {
    need_to_stop = 1;
  }
}