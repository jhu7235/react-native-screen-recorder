import React from "react";
import {
  StyleSheet,
  Text,
  View,
  CameraRoll,
  Button,
  NativeModules,
  Platform,
  DeviceEventEmitter,
  Keyboard,
} from "react-native";
import { TextInput } from "react-native-paper";

import VideoPlayer from "react-native-video-controls";
const { RecorderManager } = NativeModules;

export default class App extends React.Component {
  state = {
    videoUri: null,
    disableStart: false,
    disableStopped: true,
    disablePlayable: true,
    androidVideoUrl: null,
    keyboardIsShown: false,
  };

  start = () => {
    RecorderManager.start();
    this.setState({
      disableStart: true,
      disableStopped: false,
      disablePlayable: true,
    });
  };

  stop = () => {
    RecorderManager.stop();
    this.setState({
      disableStart: true,
      disableStopped: true,
      disablePlayable: false,
    });
  };

  play = () => {
    switch (Platform.OS) {
      case "android":
        const { androidVideoUrl } = this.state;
        if (androidVideoUrl) {
          this.setState({
            videoUri: androidVideoUrl,
            disableStart: true,
            disableStopped: true,
            disablePlayable: true,
          });
        }
        break;

      case "ios":
        CameraRoll.getPhotos({
          first: 1,
          assetType: "Videos",
        }).then((r) => {
          if (r.edges.length > 0) {
            const video = r.edges[0].node.image;
            this.setState({
              videoUri: video.uri,
              disableStart: true,
              disableStopped: true,
              disablePlayable: true,
            });
          }
        });
        break;

      default:
        break;
    }
  };

  playEnded = () => {
    this.setState({
      videoUri: null,
      disableStart: false,
      disableStopped: true,
      disablePlayable: true,
      androidVideoUrl: null,
    });
  };

  keyboardDidShow = () => {
    this.setState({ keyboardIsShown: true });
  };

  keyboardDidHide = () => {
    this.setState({ keyboardIsShown: false });
  };

  rendernControlBtnGroup = () => {
    const { disableStart, disableStopped, disablePlayable } = this.state;
    return (
      <View style={styles.footer}>
        <Button
          style={styles.button}
          disabled={disableStart}
          title="Start"
          onPress={this.start}
        />
        <Button
          style={styles.button}
          disabled={disableStopped}
          title="Stop"
          onPress={this.stop}
        />
        <Button
          style={styles.button}
          disabled={disablePlayable}
          title="Play"
          onPress={this.play}
        />
      </View>
    );
  };

  componentWillMount() {
    DeviceEventEmitter.addListener("updateFilePath", (filePath) => {
      console.log(filePath);

      this.setState({ androidVideoUrl: filePath });
    });
  }

  componentDidMount() {
    this.keyboardDidShowListener = Keyboard.addListener(
      "keyboardDidShow",
      this.keyboardDidShow
    );
    this.keyboardDidHideListener = Keyboard.addListener(
      "keyboardDidHide",
      this.keyboardDidHide
    );
  }

  componentWillUnmount() {
    this.keyboardDidShowListener.remove();
    this.keyboardDidHideListener.remove();
  }

  render() {
    const { videoUri, keyboardIsShown } = this.state;
    return (
      <View style={styles.container}>
        {Platform.OS === "ios" &&
          keyboardIsShown &&
          this.rendernControlBtnGroup()}
        <View style={styles.content}>
          {videoUri && (
            <VideoPlayer source={{ uri: videoUri }} onEnd={this.playEnded} />
          )}
          {!videoUri && (
            <TextInput
              mode="flat"
              label="I am label"
              style={styles.textInput}
              multiline
              underlineColorAndroid="white"
              onChangeText={(text) => this.setState({ text })}
              value={this.state.text}
            />
          )}
        </View>
        {this.rendernControlBtnGroup()}
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 20,
    backgroundColor: "#fff",
  },

  content: {
    flex: 1,
    padding: 20,
    justifyContent: "center",
  },

  textInput: {},

  footer: {
    backgroundColor: Platform.OS === "ios" ? "#eee" : "#fff",
    flexDirection: "row",
    alignSelf: "stretch",
    justifyContent: "center",
    paddingVertical: 20,
  },
});
