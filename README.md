# RC Car Controller App

Android frontend prototype for an RC car controller.

## Download The Project

1. Open a terminal.
2. Run:

```bash
git clone https://github.com/Aflyingdino/RC-car-controller-app.git
cd RC-car-controller-app
```

## Run On Android (USB Debugging)

1. Install Android Studio (latest stable) on macOS.
2. Install ADB on Mac:

```bash
brew install android-platform-tools
```

3. On your Android phone:
	- Enable Developer Options (tap Build Number 7 times in About Phone).
	- Turn on USB debugging.
4. Connect your phone by USB and accept the Allow USB debugging prompt.
5. In Android Studio:
	- Open this project folder.
	- Wait for Gradle sync to finish.
	- Select your connected phone as the run target.
	- Press Run for the app module.

## macOS Quick Notes

1. No OEM USB driver install is needed on Mac.
2. If your phone does not show in Android Studio, unplug/replug and re-accept the USB debugging prompt on phone.
3. If prompted on phone, choose File Transfer (MTP), not Charge only.

## Build APK And Install Manually

1. In Android Studio, open Build > Build Bundle(s) / APK(s) > Build APK(s).
2. After build finishes, click Locate.
3. Copy the generated APK to your phone.
4. On your phone, open the APK and install it.
5. If prompted, allow Install unknown apps for your file manager/browser.

## If Device Is Not Detected (macOS)

1. Try a different USB cable or port.
2. Set USB mode to File Transfer (not Charge only).
3. Verify with:

```bash
adb devices
```

4. If it shows unauthorized:
	- Disconnect USB.
	- Revoke USB debugging authorizations on your phone.
	- Reconnect and accept the trust/debug prompt again.

Your phone should appear as device, not unauthorized.

## Notes

1. This is frontend-only right now (UI, gyro steering, accelerate, brake, and dashboard animation).
2. Bluetooth connection logic is not implemented yet.