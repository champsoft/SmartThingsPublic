var fs = require('fs');
var http = require('http');

var prevFileName = '';
var okToRun = true;
var timerSeconds = 60;
var logFile = '/home/pi/Video/dir_watcher.log';  //Change to your own log file

writeToLog(new Date + ': dir_watcher initiated, waiting for camera motion...\n' + 
		'---------------------------------------------------------------------------------------------');

fs.watch('/your/capture_dir', function(event, filename) {
	//writeToLog('event is: ' + event);
	if (filename) {
	// writeToLog('filename provided: ' + filename);
	}
	else {
	// writeToLog('filename not provided');
	}
	
	//writeToLog(new Date + ': okToRun = ' + okToRun);
 	
	//Compare file names in case the watch reports a file change immediately after the timer reset
	if(okToRun && filename != prevFileName) {
		okToRun = false;
		writeToLog(new Date + ': File \'' + filename + '\' detected, starting execution...' );
		execLight();
	}
	
	prevFileName = filename;
});

function execLight() {
	writeToLog(new Date + ': Running REST function and will reset timer in ' + timerSeconds + ' seconds'); 

	resetTimer(timerSeconds);

	httpGet();	
}

function resetTimer(secs) {
        this.setTimeout(function() {
		writeToLog(new Date + ': The timer was reset in ' + secs + ' second(s).  Waiting for camera motion...\n' +
				'------------------------------------------------------------------------------------------------------');

		okToRun = true;
	}, secs * 1000);
	
}

function writeToLog(message) {
	fs.appendFile(logFile, message + '\n', function (err) {
		if (err) throw err;
  		console.log(message);
	});
}

var options = {
	hostname: 'localhost',
	port: 80,
	path:'/?access_token=place_your_access_token_here&motion_detect=true',
	agent: false
};

function httpGet() {
	 http.get(options, function(res) {
		res.on('data', function(d) {
			console.log(d);
		});
	});
}
