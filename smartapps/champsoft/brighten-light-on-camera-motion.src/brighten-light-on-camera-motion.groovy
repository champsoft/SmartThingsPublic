/**
 *  Brighten Light on Motion
 *
 *  Copyright 2015 Champion Software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Base from: App Endpoint API Access Example
 *
 *  Base Author: SmartThings
 */
 

// Automatically generated. Make future change here.
definition(
    name: "Brighten Light on Camera Motion",
    namespace: "champsoft",
    author: "Champsoft",
    description: "Brighten lights to a preset level when dmotion is detected by a camera.  Set the lights' state to their original states after a preset number of minutes is reached",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
	section("Enable app...") {
    	input "appEnabled", "bool", title: "Enable this app", required: true, defaultValue: true
    }
	section("Allow Endpoint to Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Set of Hue Bulbs?", required:true, multiple:true
			input "delayMins", "enum", title: "Brightness Duration", required: true, options: [[1:"1 minute"], [2:"2 minutes"], [3:"3 minutes"], [4:"4 minutes"], [5:"5 minutes"]], defaultValue: 3
            input "sunsetEnabled", "bool", title: "Enable Only At Sunset", required: true, defaultValue: true
	}
    section("Choose light effects...") {
    		input "keepSameColor", "bool", title: "When brightened, keep the same color as when normal?", required: true, defaultValue: false
			input "color", "enum", title: "or choose a Hue color?", required: false, multiple:false, defaultValue: "Blue", options: [
				["Soft White":"Soft White"],
				["White":"White - Concentrate"],
				["Daylight":"Daylight - Energize"],
				["Warm White":"Warm White - Relax"],
                ["Blue":"Blue - default"],
				"Red","Green","Yellow","Orange","Purple","Pink"]
			input "lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: 50
            input "saturationLevel", "number", title: "Saturation Level?", description: "Between 1 and 100", required: false, defaultValue: 60
            
	}
    section("Pause App on False Detections...") {
		input "detectIntervalSeconds", "enum", title: "Detection Interval Seconds", defaultValue: 30000, required: true, options: [[20000:"20 seconds"], [30000:"30 seconds"], [40000:"40 seconds"], [50000:"50 seconds"], [60000:"60 seconds"]]
        input "occurenceMax", "number", title: "Maximum Detections?", defaultValue: 5, description: "Maximum consecutive occurences that triggers false alarm", required: true
        input "resetMinutes", "number", title: "Minutes Until Reset?", defaultValue: 120, description: "Elapsed time before the app is re-enabled", required: true

    }
        
}

mappings {

	path("/hues") {
		action: [
			GET: "listHues"
		]
	}
	path("/hues/:id") {
		action: [
			GET: "showHue"
		]
	}
	path("/hues/:id/:command") {
		action: [
			GET: "updateHue"
		]
	}
}

def installed() {initialize()}

def updated() {
    initialize()
}

def initialize() {
	state.lastLightReset = 0
    state.switchOccurence = 0
    state.disableTime = 0
}

//hues
def listHues() {
	hues.collect{device(it,"hue")}
}

def showHue() {
	show(hues, "hue")
}

void updateHue() {
	//Called from oAuth
	if(appEnabled && ((sunsetEnabled && isSunset()) || !sunsetEnabled))
    	switchOn(hues)
}

def deviceHandler(evt) {}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}

private device(it, type) {
	it ? [id: it.id, label: it.label, type: type] : null
}

private convertColor() {
    def hueColor = 0
    switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
        default:
        	hueColor = 70
            saturation = 60
	}
    return hueColor
}

private void switchOn(devices) {

	log.debug "update, request: params: ${params}, devices: $devices.id"

	setOccurenceCounter()
    
    if(state.switchOccurence >= occurenceMax) {
    	def resetInterval = resetMinutes as Integer
        resetInterval *= 60 * 1000
        if(!isElapsed(resetInterval)) {
        	log.debug "Too many false detections.  The app is paused for $resetMinutes"
        	return
        }
    }
    
    def anyPowerSwitch = setPreviousValues(devices)
    
    //If occurence count < occurenceMax - To stop continious false detections from brightening the lights.  ie, when snowing or raining
    if(state.switchOccurence < occurenceMax) {

		// Set power on if at least one light is off. Prevent performance issues by not always switching lights on
    	if(anyPowerSwitch == "off")
    		hues*.on()
        
    	//if user did not select to keep the same color, process by batch
    	if(!keepSameColor){
    		hues*.setLevel(lightLevel as Integer)
    		hues*.setColor([hue:convertColor(), saturation:saturationLevel]) //, level:lightLevel as Integer
        	log.debug "User selected color values are used for all hues: color = ${color}, level = ${lightLevel}, staturation = $saturationLevel"
    	}
   		else { 
        	//Set to user selected light level only, when keeping the same colors.  Do not change color and saturation
        	hues*.setLevel(lightLevel as Integer)
			log.debug "The new values are the same as the previous values, except for the level which is now: $lightLevel"
    	}
    }
    else {
    	log.debug "Maximum occurence limit reached.  Lights will not update until the counter is reset at sunrise or set threshold..."
    }

	def delay = delayMins as Integer
    delay *= 60
    log.debug "Will update previous values in $delay seconds..."

	runIn(delay, resetToPreviousValues, [overwrite: true])
}

def setOccurenceCounter() {
	//Check to see if detections are taking place too often
    //If they are, app will stop working for a specified time
    if(!isElapsed(detectIntervalSeconds as Integer) && state.lastLightReset > 0) {
    	state.switchOccurence = state.switchOccurence + 1
        log.debug "Occurence count: ${state.switchOccurence}"
    }
    else {
    	state.switchOccurence = 0
        log.debug "Occurence count set to 0"
    }
}

def setPreviousValues(devices) {
    // To check if any light is switched off
    def anySwitch = "on"
    
    //Check if 15 seconds have passed
    //If so, set the previous values 
    if(isElapsed(15000)) {
    	state.previous = [:]
        hues.each {
        	state.previous[it.id] = [
				switch: it.currentValue("switch"),
				level : it.currentValue("level"),
				hue: it.currentValue("hue"),
				saturation: it.currentValue("saturation")
			]
            
            anySwitch = anySwitch == "off" ? "off" : it.currentValue("switch")
            log.debug "Previous values for ${it.id}: ${state.previous[it.id]}"
        }
    }
    else {
    	log.debug "Previous values were not set: 15 seconds not elapsed"
    }
    
    return anySwitch
}

def resetToPreviousValues() {
	def allPreviousColorsMatched = true
    def lastColor = 0
    def lastSaturation = 0
    def lastLevel = 0
    def lastState = ""
    
    def isDayLightSelect = sunsetEnabled && !isSunset()
    log.debug "isDayLightSelect: $isDayLightSelect"
    
    //Check to see if all previous colors are the same
    settings.hues.each {
    	if(lastColor == 0)
            lastColor = state.previous[it.id].hue
        
        allPreviousColorsMatched = !allPreviousColorsMatched ? false : state.previous[it.id].hue == lastColor
        //Keep all previous values
        lastColor = state.previous[it.id].hue
        lastSaturation = state.previous[it.id].saturation
        lastLevel = state.previous[it.id].level
        lastState = state.previous[it.id].switch
    }
    
    if(state.switchOccurence < occurenceMax) {
		//If all previous colors matched, process in batch
    	if(allPreviousColorsMatched) {
    		hues*.setColor([hue:lastColor, level:lastLevel, saturation:lastSaturation])
        	if(lastState == "off" || isDayLightSelect)
        		hues*.off()
        	log.debug "All previous colors matched processing with the following: switch:${lastState}, hue:${lastColor}, level:${lastLevel}, saturation:${lastSaturation}"
    	}
    	//Otherwise process colors and power state individually
    	else {
   			settings.hues.each {
        		if(!keepSameColor) {
            		//Change the hue and saturation only if user did not select keeping the same color
        			it.setHue(state.previous[it.id].hue)
    				it.setSaturation(state.previous[it.id].saturation)
        		}
    			it.setLevel(state.previous[it.id].level)
    		}
    
    		//Previous values were "off or it's day light, turn off the light
        	if(state.previous[it.id].switch=="off" || isDayLightSelect) {
        		hues.each {
            		it.off()
        		}
				log.debug "Previous value applied: ${it.id}: ${state.previous[it.id]}"
         	}
    	}
    	state.lastLightReset = now()
    }
}

private isElapsed(threshold) {
	//log.debug "delay check"
    //Values have never been reset before
    if(state.lastLightReset == 0) {
    	log.debug "First time reset."
        //state.lastLightReset = now()
        return true;
    }
	
    def elapsed = now() - state.lastLightReset

    if (elapsed > threshold) {
        log.debug "Time has elapse time : $elapsed ms, with threshold of $threshold ms"
        return true
    } else {
        log.debug "Time has NOT elapsed: $elapsed ms, with threshold of $threshold ms"
	    return false
	}
}

private isSunset() {
 	def riseTime = getSunriseAndSunset(zipCode: location.zipCode).sunrise
    def setTime = getSunriseAndSunset(zipCode: location.zipCode).sunset
    def timeNow = now()
    
    // if the current time is past the sunrise time, update it tomorrow's sunrise time
    // else set the sunset time to yesterday's
    if(timeNow >= riseTime.getTime()) {
    	riseTime++
    }
    else {
    	setTime--
    }
    
     //Sunset state is true when the current time is between sunset and sunrise
    if(timeNow >= setTime.getTime() && timeNow < riseTime.getTime()) {
    	log.debug("It is sunset")
        return true
    }
    else {
    	log.debug("It is not sunset")
    	return false
    }
}
