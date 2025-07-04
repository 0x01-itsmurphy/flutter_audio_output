import 'package:flutter/material.dart';
import 'package:flutter_audio_output/flutter_audio_output.dart';
import 'dart:async';
import 'package:just_audio/just_audio.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Audio Output Example',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const AudioOutputExample(),
    );
  }
}

class AudioOutputExample extends StatefulWidget {
  const AudioOutputExample({super.key});

  @override
  State<AudioOutputExample> createState() => _AudioOutputExampleState();
}

class _AudioOutputExampleState extends State<AudioOutputExample> {
  late AudioPlayer _audioPlayer;
  AudioInput? _currentOutput;
  List<AudioInput> _availableDevices = [];
  bool _isPlaying = false;
  bool _isLoading = false;
  String? _errorMessage;
  String? _platformVersion;

  @override
  void initState() {
    super.initState();
    _initializeAudioPlayer();
    _initializeAudioOutput();
  }

  Future<void> _initializeAudioPlayer() async {
    _audioPlayer = AudioPlayer();

    // Listen to player state changes
    _audioPlayer.playerStateStream.listen((state) {
      if (mounted) {
        setState(() {
          _isPlaying = state.playing;
        });
      }
    });

    try {
      await _audioPlayer.setAudioSource(AudioSource.uri(
        Uri.parse('asset:///assets/Sunflower.mp3'),
      ));
    } catch (e) {
      _showError('Failed to load audio: $e');
    }
  }

  Future<void> _initializeAudioOutput() async {
    try {
      // Get platform version
      _platformVersion = await FlutterAudioOutput.getPlatformVersion();

      // Set up listener for audio device changes
      FlutterAudioOutput.setListener(() async {
        await _refreshAudioDevices();
        _showSnackBar('Audio device changed');
      });

      // Initial load of audio devices
      await _refreshAudioDevices();
    } catch (e) {
      _showError('Failed to initialize audio output: $e');
    }
  }

  Future<void> _refreshAudioDevices() async {
    if (!mounted) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final current = await FlutterAudioOutput.getCurrentOutput();
      final available = await FlutterAudioOutput.getAvailableInputs();

      if (mounted) {
        setState(() {
          _currentOutput = current;
          _availableDevices = available;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _switchToDevice(AudioPort port) async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });

      bool success = false;
      String deviceName = '';

      switch (port) {
        case AudioPort.receiver:
          success = await FlutterAudioOutput.changeToReceiver();
          deviceName = 'Receiver';
          break;
        case AudioPort.speaker:
          success = await FlutterAudioOutput.changeToSpeaker();
          deviceName = 'Speaker';
          break;
        case AudioPort.headphones:
          success = await FlutterAudioOutput.changeToHeadphones();
          deviceName = 'Headphones';
          break;
        case AudioPort.bluetooth:
          success = await FlutterAudioOutput.changeToBluetooth();
          deviceName = 'Bluetooth';
          break;
        default:
          _showError('Unsupported audio port: $port');
          return;
      }

      if (success) {
        _showSnackBar('Successfully switched to $deviceName');
        await _refreshAudioDevices();
      } else {
        _showError('Failed to switch to $deviceName');
      }
    } catch (e) {
      _showError('Error switching to ${port.name}: $e');
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _showError(String message) {
    if (mounted) {
      setState(() {
        _errorMessage = message;
      });
      _showSnackBar(message, isError: true);
    }
  }

  void _showSnackBar(String message, {bool isError = false}) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: isError ? Colors.red : Colors.green,
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  IconData _getIconForPort(AudioPort port) {
    switch (port) {
      case AudioPort.receiver:
        return Icons.phone;
      case AudioPort.speaker:
        return Icons.volume_up;
      case AudioPort.headphones:
        return Icons.headphones;
      case AudioPort.bluetooth:
        return Icons.bluetooth;
      default:
        return Icons.device_unknown;
    }
  }

  Color _getColorForPort(AudioPort port) {
    switch (port) {
      case AudioPort.receiver:
        return Colors.blue;
      case AudioPort.speaker:
        return Colors.orange;
      case AudioPort.headphones:
        return Colors.purple;
      case AudioPort.bluetooth:
        return Colors.indigo;
      default:
        return Colors.grey;
    }
  }

  @override
  void dispose() {
    _audioPlayer.dispose();
    FlutterAudioOutput.removeListener();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Flutter Audio Output'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _refreshAudioDevices,
            tooltip: 'Refresh Devices',
          ),
        ],
      ),
      body: Column(
        children: [
          // Platform version info
          if (_platformVersion != null)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              color: Colors.grey[100],
              child: Text(
                'Platform: $_platformVersion',
                style: Theme.of(context).textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
            ),

          // Error message display
          if (_errorMessage != null)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              color: Colors.red[100],
              child: Row(
                children: [
                  const Icon(Icons.error, color: Colors.red),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _errorMessage!,
                      style: const TextStyle(color: Colors.red),
                    ),
                  ),
                ],
              ),
            ),

          // Audio controls
          Container(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                Text(
                  'Audio Controls',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    ElevatedButton.icon(
                      onPressed: _isPlaying ? null : () => _audioPlayer.play(),
                      icon: const Icon(Icons.play_arrow),
                      label: const Text('Play'),
                    ),
                    ElevatedButton.icon(
                      onPressed: _isPlaying ? () => _audioPlayer.pause() : null,
                      icon: const Icon(Icons.pause),
                      label: const Text('Pause'),
                    ),
                    ElevatedButton.icon(
                      onPressed: () => _audioPlayer.stop(),
                      icon: const Icon(Icons.stop),
                      label: const Text('Stop'),
                    ),
                  ],
                ),
              ],
            ),
          ),

          const Divider(),

          // Current output display
          if (_currentOutput != null)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Current Output Device',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Icon(
                            _getIconForPort(_currentOutput!.port),
                            color: _getColorForPort(_currentOutput!.port),
                            size: 32,
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  _currentOutput!.name,
                                  style: Theme.of(context).textTheme.titleSmall,
                                ),
                                Text(
                                  _currentOutput!.port.name.toUpperCase(),
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // Available devices list
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Available Audio Devices',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  if (_isLoading)
                    const Center(
                      child: CircularProgressIndicator(),
                    )
                  else if (_availableDevices.isEmpty)
                    const Center(
                      child: Text('No audio devices available'),
                    )
                  else
                    Expanded(
                      child: ListView.builder(
                        itemCount: _availableDevices.length,
                        itemBuilder: (context, index) {
                          final device = _availableDevices[index];
                          final isCurrentDevice = _currentOutput != null &&
                              device.name == _currentOutput!.name &&
                              device.port == _currentOutput!.port;

                          return Card(
                            color: isCurrentDevice
                                ? _getColorForPort(device.port).withOpacity(0.1)
                                : null,
                            child: ListTile(
                              leading: Icon(
                                _getIconForPort(device.port),
                                color: _getColorForPort(device.port),
                                size: 32,
                              ),
                              title: Text(device.name),
                              subtitle: Text(
                                device.port.name.toUpperCase(),
                                style: TextStyle(
                                  color: _getColorForPort(device.port),
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                              trailing: isCurrentDevice
                                  ? const Icon(
                                      Icons.check_circle,
                                      color: Colors.green,
                                    )
                                  : const Icon(Icons.arrow_forward_ios),
                              onTap: isCurrentDevice
                                  ? null
                                  : () => _switchToDevice(device.port),
                            ),
                          );
                        },
                      ),
                    ),
                ],
              ),
            ),
          ),

          // Quick action buttons
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                Text(
                  'Quick Actions',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: _isLoading
                            ? null
                            : () => _switchToDevice(AudioPort.receiver),
                        icon: const Icon(Icons.phone),
                        label: const Text('Receiver'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.blue.withOpacity(0.1),
                          foregroundColor: Colors.blue,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: _isLoading
                            ? null
                            : () => _switchToDevice(AudioPort.speaker),
                        icon: const Icon(Icons.volume_up),
                        label: const Text('Speaker'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.orange.withOpacity(0.1),
                          foregroundColor: Colors.orange,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
