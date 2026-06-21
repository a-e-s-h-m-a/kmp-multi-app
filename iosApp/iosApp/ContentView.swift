import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("AppOne")
                .font(.largeTitle)
                .bold()
            Text("Select the AppOne or AppTwo scheme to run the shared multi-app experience.")
        }
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
