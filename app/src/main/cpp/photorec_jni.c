#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int photorec_cli_main(int argc, char **argv);
extern int need_to_stop;

static pthread_mutex_t scan_mutex = PTHREAD_MUTEX_INITIALIZER;

JNIEXPORT jint JNICALL
Java_com_example_tools_recover_PhotoRecNative_runScan(
    JNIEnv *env,
    jobject receiver,
    jint source_fd,
    jstring output_directory) {
  (void)receiver;
  if (pthread_mutex_trylock(&scan_mutex) != 0) {
    return -4;
  }

  need_to_stop = 0;
  jint result = -1;
  const char *output_path = (*env)->GetStringUTFChars(env, output_directory, NULL);
  if (output_path == NULL) {
    goto cleanup;
  }

  struct stat source_info;
  struct stat output_info;
  if (fstat(source_fd, &source_info) != 0 || lseek(source_fd, 0, SEEK_CUR) < 0 ||
      stat(output_path, &output_info) != 0 || !S_ISDIR(output_info.st_mode)) {
    result = -2;
    goto release_output_path;
  }

  char source_path[64];
  if (snprintf(source_path, sizeof(source_path), "/proc/self/fd/%d", source_fd) < 0) {
    result = -3;
    goto release_output_path;
  }

  char *arguments[] = {
      "photorec",
      "/nolog",
      "/d",
      (char *)output_path,
      "/cmd",
      source_path,
      "search",
      NULL};
  result = photorec_cli_main(7, arguments);
  if (need_to_stop != 0) {
    result = -5;
  }
  need_to_stop = 0;

release_output_path:
  (*env)->ReleaseStringUTFChars(env, output_directory, output_path);
cleanup:
  pthread_mutex_unlock(&scan_mutex);
  return result;
}

JNIEXPORT void JNICALL
Java_com_example_tools_recover_PhotoRecNative_cancelScan(JNIEnv *env, jobject receiver) {
  (void)env;
  (void)receiver;
  need_to_stop = 1;
}