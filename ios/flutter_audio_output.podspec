Pod::Spec.new do |s|
  s.name             = 'flutter_audio_output'
  s.version          = '0.0.3'
  s.summary          = 'A Flutter plugin for managing audio output routing.'
  s.description      = <<-DESC
A Flutter plugin that allows you to manage audio output routing, including switching between speaker, receiver, headphones, and Bluetooth devices.
                       DESC
  s.homepage         = 'https://github.com/itsmurphy/flutter_audio_output'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'itsmurphy' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '12.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 
    'DEFINES_MODULE' => 'YES', 
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386',
    'SWIFT_VERSION' => '5.0'
  }
  s.swift_version = '5.0'
end
