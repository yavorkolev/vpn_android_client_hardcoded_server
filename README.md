# Android Java Vpn Client Android Studio Projeect Example
To use it have to add your server configuration: app\src\main\assets\your-file.ovmp //for example!
Have to add your server IP in only one place in the code of "MainActivity" 
onReceive of broadcastReceiver if(!binding.yourIpValueTextView.getText().equals("YOUR SERVER IP")) { ... 
