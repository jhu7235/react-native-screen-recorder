import { AppRegistry } from "react-native";
import * as React from "react";
import { Provider as PaperProvider } from "react-native-paper";
import App from "./App";

export default function Main() {
  return (
    <PaperProvider>
      <App />
    </PaperProvider>
  );
}

AppRegistry.registerComponent("screenrecorder", () => Main);
