import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        PlatformContext.shared.initialize(context: NSObject())
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
