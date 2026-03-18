import SwiftUI
import SwiftData

@main
struct SitePinApp: App {
    @AppStorage("appColorScheme") private var appColorScheme: String = "system"

    var body: some Scene {
        WindowGroup {
            ProjectListView()
                .preferredColorScheme(colorScheme)
        }
        .modelContainer(for: [Project.self, PlanDocument.self, Pin.self, PinPhoto.self, PinComment.self])
    }

    private var colorScheme: ColorScheme? {
        switch appColorScheme {
        case "light": return .light
        case "dark": return .dark
        default: return nil
        }
    }
}
