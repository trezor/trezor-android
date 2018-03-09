How to bundle TREZOR firmware to TREZOR Manager APK:

* remove old firmware and put new firmware to this folder
  (trezor-android/trezor-app/src/main/assets/firmware or firmware-v2 for the new "T" model)
* filename of the firmware has to be trezor-X.Y.Z.bin,
  where X, Y a Z are major, minor a patch fields of version respectively
* release notes has to be saved in releases.json file using this pattern (you can keep unused json fields):
[
	{
		"required": true,
		"version": [2, 0, 5],
		"fingerprint": "851172eab96c07bf9efb43771cb0fd14dc0320a68de047132c7bd787a1ad64e9",
		"changelog": "* first public release"
	},
  ...
]
* the newest version in the list must be the same as the bundled firmware 
* all text files are using UTF-8 encoding and LF (Unix) endlines
