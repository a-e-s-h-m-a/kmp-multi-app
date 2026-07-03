import SwiftUI

extension NativeTheme {
    var primaryColor: Color {
        switch self {
        case .boutique:
            Color(red: 0.45, green: 0.32, blue: 0.12)
        case .broadline:
            Color(red: 0.08, green: 0.36, blue: 0.63)
        case .operations:
            Color(red: 0.22, green: 0.42, blue: 0.13)
        }
    }

    var backgroundColor: Color {
        switch self {
        case .boutique:
            Color(red: 1.00, green: 0.98, blue: 0.95)
        case .broadline:
            Color(red: 0.97, green: 0.98, blue: 1.00)
        case .operations:
            Color(red: 0.97, green: 0.98, blue: 0.96)
        }
    }
}
