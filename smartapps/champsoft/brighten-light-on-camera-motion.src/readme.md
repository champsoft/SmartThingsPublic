### This program uses the fs.watch module method in node.js on a Raspberry Pi to detect file changes on a chosen local or network directory.  It then notifies a running php script that triggers an OAuth enabled smart app to take action in SmartThings.  This was put together using a D-Link DCS-2330L, but it should work with any camera that outputs a video or image to disk when motion is detected. ###
Install the following modules and libraries in your RPi:

*	If not already installed, setup, configure Apache and PHP (https://www.raspberrypi.org/documentation/remote-access/web-server/apache.md)

*	Install libcurl (eg php5-curl).

*	Install the latest node.js.

Download and configure the following:

*	Set up a php site and use the supplied [index.php]( https://github.com/champsoft/SmartThingsPublic/blob/master/smartapps/champsoft/brighten-light-on-camera-motion.src/index.php).

*	Place the supplied [dir_watcher.js]( https://github.com/champsoft/SmartThingsPublic/blob/master/smartapps/champsoft/brighten-light-on-camera-motion.src/dir_watcher.js) script in a directory to be run by command line, or by a looper such as [forever]( https://github.com/foreverjs/forever).

*	Install the smart app (https://github.com/champsoft/SmartThingsPublic/tree/master/smartapps/champsoft/brighten-light-on-camera-motion.src) in SmartThings.

*	Configure [OAuth]( https://community.smartthings.com/t/tutorial-creating-a-rest-smartapp-endpoint/4331) for the smart app.
