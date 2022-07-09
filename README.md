# flutter_audio_output

-   Get the current output audio device
-   Listening to input device changes
-   Switching audio input/output devices

## Getting Started

```
import 'package:flutter/material.dart';
import 'package:flutter_audio_output/flutter_audio_output.dart';
import 'dart:async';
import 'package:logger/logger.dart';

import 'package:just_audio/just_audio.dart';

void main() => runApp(const MyApp());

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late AudioPlayer _audioPlayer;
  final logger = Logger();

  late AudioInput _currentInput = const AudioInput("unknown", 0);
  late List<AudioInput> _availableInputs = [];
  bool isSpeaker = true;

  @override
  void initState() {
    _audioPlayer = AudioPlayer();
    _audioPlayer
        .setAudioSource(AudioSource.uri(
      Uri.parse('asset:///assets/Sunflower.mp3'),
    ))
        .catchError((error) {
      print("An error occured $error");
    });
    _audioPlayer.play();

    init();
    super.initState();
  }

  Future<void> init() async {
    FlutterAudioOutput.setListener(() async {
      logger.d("-----changed-------");
      await _getInput();
      setState(() {});
    });

    await _getInput();
    if (!mounted) return;
    setState(() {});
  }

  _getInput() async {
    _currentInput = await FlutterAudioOutput.getCurrentOutput();
    logger.w("current:$_currentInput");
    _availableInputs = await FlutterAudioOutput.getAvailableInputs();
    logger.w("available $_availableInputs");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Column(
            children: <Widget>[
              Text(
                "current output:${_currentInput.name} ${_currentInput.port}",
              ),
              Divider(),
              Expanded(
                child: ListView.builder(
                  itemBuilder: (_, index) {
                    AudioInput input = _availableInputs[index];
                    return Row(
                      children: <Widget>[
                        Expanded(child: Text(input.name)),
                        Expanded(child: Text("${input.port}")),
                      ],
                    );
                  },
                  itemCount: _availableInputs.length,
                ),
              ),
              Container(
                height: 400,
                child: Row(
                  children: [
                    IconButton(
                      onPressed: () {
                        _audioPlayer.pause();
                      },
                      icon: Icon(Icons.pause_circle),
                    ),
                    IconButton(
                      onPressed: () {
                        _audioPlayer.play();
                      },
                      icon: Icon(Icons.play_circle),
                    ),
                    IconButton(
                      onPressed: () async {
                        await _getInput();
                        if (_currentInput.port == AudioPort.speaker) {
                          isSpeaker =
                              await FlutterAudioOutput.changeToReceiver();
                          setState(() {
                            isSpeaker = false;
                          });
                          print("change to Receiver :$isSpeaker");
                        } else {
                          isSpeaker =
                              await FlutterAudioOutput.changeToSpeaker();
                          setState(() {
                            isSpeaker = true;
                          });
                          print("change to Speaker :$isSpeaker");
                        }
                      },
                      icon: isSpeaker == false
                          ? const Icon(
                              Icons.volume_off,
                              color: Colors.black,
                            )
                          : const Icon(
                              Icons.volume_up,
                              color: Colors.black,
                            ),
                    ),
                  ],
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
```
