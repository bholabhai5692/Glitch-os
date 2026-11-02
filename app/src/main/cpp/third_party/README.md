# Vendored Native Sources

## TestDisk and PhotoRec

- Upstream: https://github.com/cgsecurity/testdisk
- Revision: `f9e57dc98be47a952078e6a7f54cb134bddfb141`
- License: GPL-2.0-or-later; see `testdisk/COPYING`.
- The CMake target uses the upstream PhotoRec source list and recovery engine. The Android UI accepts user-selected disk images, opens them read-only, and exports recovered files to a user-selected folder.
- Android integration does not access protected raw device partitions or perform TestDisk partition-table writes.
