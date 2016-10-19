/**
 *  Turn light on with motion/contact for x minutes if it is dark based on light sensor
 *
 *   Written by Tuffcalc
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
 */
 
 /*
 	This code is modified by champsoft, to meet specific location needs
    - Added sunrise and sunset mode features.
    - Removed app states that stored settings and switch, sensor status
 */

definition(
    name: "Dimmer (Motion, Contact & Lux)",
    namespace: "champsoft",
    author: "tuffcalc",
    description: "Turn on lights temporarily when there is motion but only if it is dark according to light sensor, turn light off if above lux setpoint.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",  
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    //todo:  replace icon with something appropriate.  


preferences {
	section("Enable App") {
        input "appEnabled", "bool", title: "ON=Enabled, OFF=Disabled", required: true, defaultValue: true
	}
    section("Select Motion Sensor(s) you want to Use") {
        input "motions", "capability.motionSensor", title: "Motion Detector", required: false, multiple: true
	}
    section("Select Contact Sensor(s) you want to use") {
        input "contacts", "capability.contactSensor", title: "Contact Sensor", required: false, multiple: true
        }
    section("Select Dimmers you want to Use") {
        input "switches", "capability.switchLevel", title: "Dimmer Switches", required: true, multiple: true
	}
    section ("Set Brightness for motion/contact triggered light and on time after motion/contact stops") {

        input "BrightLevelStr", "number", title: "Brightness Level %", required: true, 
        	defaultValue: "100"

        input "DelayMinStr", "number", title: "Switched On Delay After Motion/Contact Stops (minutes)", required: true, 
        	defaultValue: "5"
            
        input "setMode", "mode", title: "Enable Only During this Mode", required:false
        
        //Added by Champsoft - to allow the option of motion to turn on the lights only between sunset and sunrise
        input "sunPositionEnabled", "bool", title: "Enable Only Between Sunset and Sunrise", required: true, 
        	defaultValue: false
            
            }
  
    section ("Ignore motion/contact if lux is above this value") {
    	input "LightMeter", "capability.illuminanceMeasurement", title: "Light Meters", required: true, multiple: false
		input "LuxSetPointStr", "number", title: "Lux level", required: true, 
        	defaultValue: "200"
           	}

	// start light meter on code
	section ("Turn lights off on lux event if lux is above this value (optional)") {
    	input "LuxSetPoint1Str", "number", title: "Lux level", required: false, 
        	defaultValue: ""
           	}         
	// end light meter on code

}


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "initialize()"
    subscribe(motions, "motion.active", handleContactEvent)
    subscribe(motions, "motion.inactive", handleEndContactEvent)
    subscribe(contacts, "contact.open", contactopen)
    subscribe(contacts, "contact.closed", contactclosed)
    subscribe(location, "mode", modeChangeHandler)
    subscribe(LightMeter, "illuminance", LuxChange)
}

///// end of subscriptions

def LuxChange(evt) {
	log.debug "LuxChange(evt)"
	log.debug "Current Lux is $LightMeter.currentIlluminance"

	if(!LuxSetPoint1Str) {
        log.debug "Luxmeter not used, do nothing." 
    }
    else {
        if(LightMeter.currentIlluminance > LuxSetPointStr) {
            log.debug "Lights already turned off due to current lux over set lux, do nothing."
        }
                         
        else {
        	log.debug "Current Lux: ($LightMeter.currentIlluminance) Lux_Over_Setpoint: ($LuxSetPoint1Str)"
            if(LightMeter.currentIlluminance > LuxSetPoint1Str) {
        		log.debug "It is too bright in the room, turning lights off."
            	//Placed on 2 second timer to make sure previously scheduled off timers are unscheduled
            	runIn(2, switchLightsOff)
        	}
         	else {
        		log.debug "Current Lux ($LightMeter.currentIlluminance) below required Lux_Over_Setpoint ($LuxSetPoint1Str), doing nothing."

        	}
       }
	}
}

def contactopen(evt) {
	log.debug "Contact/Motion Activated"
	handleContactEvent()
}

def contactclosed(evt) {
	log.debug "Contact/Motion Deactivated"
	handleEndContactEvent()
}

def handleContactEvent(evt) {
	log.debug "Contact/Motion activated."
	if(settings.appEnabled) {
    	//If a mode is set, compare to current mode.
      	if((!settings.setMode) || (location.mode == settings.setMode)) {
    		// If the switch is on and sunset state is true, or if the switch is off
    		if((settings.sunPositionEnabled && isSunset()) || !settings.sunPositionEnabled) {
        		log.debug "Current Lux: ($LightMeter.currentIlluminance), Setpoint: ($LuxSetPointStr)"
        		if(LightMeter.currentIlluminance < LuxSetPointStr) {
        			log.debug "Current lux ($LightMeter.currentIlluminance) below Setpoint ($LuxSetPointStr), attempting to turn lights on."
                    switchLightsOn()
         		}
        		else {
        			log.debug "Current lux ($LightMeter.currentIlluminance) above Setpoint ($LuxSetPointStr), so doing nothing."
        		}
    		}
    		else {
        		log.debug "Sensor activated but, it is not sunset, do nothing"
    		}
    	}
        else {
        	log.debug "sensor activated, do nothing -> current mode = $location.mode, set mode = $settings.setMode"
        }
	}
    else {
    	log.debug "App not enabled, do nothing"
    }
}

def handleEndContactEvent(evt) {
	log.debug "Contact/Motion deactivated."
	if(settings.appEnabled) {
    	//If a mode is set, compare to current mode.
      	if((!settings.setMode) || (location.mode == settings.setMode)) {
    		// If the switch is on and sunset state is true, or if the switch is off or the lights where switched on by this app
    		if((settings.sunPositionEnabled && isSunset()) || !settings.sunPositionEnabled || state.lightSwitchedOn) {
    			runIn((DelayMinStr*60), switchLightsOff)
    			log.debug "Starting OFF timer."
                }
    		else {
        		log.debug "Sensor deactivated but, it is not sunset, do nothing"
    		}
    	}
        else {
        	log.debug "sensor deactivated, do nothing -> current mode = $location.mode, set mode = $settings.setMode"
        }
	}
    else {
    	log.debug "App not enabled, do nothing"
    }
}

def modeChangeHandler(evt) {
	log.debug "Mode Changed... switching lights off"
    runIn((DelayMinStr*60), switchLightsOff)
}

def switchLightsOff() {   
    switches.findAll {
    	if (it.currentSwitch == "on") {
        	log.debug "Switching OFF: ${it.label}"
  	       	it.off()
        }
        else
        	log.debug "Switch $it.label is already off, do nothing with it"
    }
    state.lightSwitchedOn = false
}

def switchLightsOn() {
	unschedule(switchLightsOff)
    switches.findAll {
    	if (it.currentSwitch == "off") {
        	log.debug "Switching ON: ${it.label}"
  	       	it.setLevel(BrightLevelStr)
        }
        else
        	log.debug "Switch $it.label is already on, do nothing with it"
    }
    state.lightSwitchedOn = true
}

def isSunset() {
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