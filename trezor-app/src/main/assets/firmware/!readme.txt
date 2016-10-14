How to bundle TREZOR firmware to TREZOR Manager APK:

* remove old firmware and put new firmware to this folder
  (trezor-android/trezor-app/src/main/assets/firmware)
* filename of the firmware has to be trezor-X.Y.Z.bin,
  where X, Y a Z are major, minor a patch fields of version respectively
* there could be only ONE firmware file present!
* firmware changelog should be put in changelog.txt on separate lines
* firmware fingerprint should be stored in fingerprint.txt
  (4 lines with 16 hexa digits)
* all text files are using UTF-8 encoding and LF (Unix) endlines
