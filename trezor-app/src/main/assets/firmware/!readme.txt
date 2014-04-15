Popis bundlovani nove verze firmwaru do APK instalacniho souboru Trezor Manageru:

Pri aktualizaci firmwaru je treba smazat stary a nahrat novy firmware do teto slozky (trezor-android/trezor-app/src/main/assets/firmware).
Jmeno souboru musi byt: trezor-X.Y.Z.bin, kde X, Y a Z jsou major, minor a patch cisla verze firmwaru.
Takovy soubor zde muze byt maximalne jeden!
Zmeny ve firmwaru je treba napsat do souboru changelog.txt, na jednotlive radky. Kodovani souboru musi byt UTF-8 a oddelovace radku znaky LF (tzn. podle unixu)
Fingerprint firmwaru (pro kontrolni zobrazeni pri aktualizaci na displeji telefonu) je treba ulozit do souboru fingerprint.txt - naformatovany tak, jak jej chceme zobrazit (tzn. nejspis 4 radky po 16 znacich)