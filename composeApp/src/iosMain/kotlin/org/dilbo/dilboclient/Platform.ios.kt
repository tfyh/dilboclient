package org.dilbo.dilboclient

// TODO for iOS implementation uncomment the following import
// import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    // TODO for iOS implementation replace the name-override
    // override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val name: String = "not supported."
}

actual fun getPlatform(): Platform = IOSPlatform()