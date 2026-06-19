import SwiftUI
import SharedLogic

struct ContentView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("AppOne")
                .font(.largeTitle)
                .bold()
            Text("The iOS multi-app SwiftUI source samples live in iosApp/SharedIOS, iosApp/AppOne, and iosApp/AppTwo.")
            Text(Greeting().greet())
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
