/**
 *  Off Switching Delay
 *
 *  Copyright 2017 pchampsoft
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
definition(
    name: "Off Switching Delay",
    namespace: "champsoft",
    author: "pchampsoft",
    description: "Turn on/off target switch(es) when a trigger switch is powered on/off. The target switches are delayed when the trigger switch is turning off.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Turn switch ON also") {
    	input "onAlso", "bool", title: "Switch devices on as well?", required: true, defaultValue: true
    }
    
    section("Select the target switches that you want to delay on power off") {
        input "targetSwitches", "capability.switch", title: "Target Switches", required: true, multiple: true
        input "delay", "number", title: "Switched Off Delay After Trigger is Switched Off (seconds)", required: true, 
        	defaultValue: "30"
	}
    
    section("Select the the trigger switch") {
        input "triggerSwitch", "capability.switch", title: "Trigger Switch", required: true
	}
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
	subscribe(triggerSwitch, "switch.on", switchOnHandler)
    subscribe(triggerSwitch, "switch.off", switchOffHandler)
}

def switchOnHandler(evt) {
	//If there is an active off switch timer, cancel it
    unschedule(turnSwitchesOff)
	turnSwitchesOn()
}

def switchOffHandler(evt) {
  	runIn(delay, turnSwitchesOff)
   	log.trace "Starting OFF timer."
}

def turnSwitchesOn() {
    if(onAlso) {
    	targetSwitches.findAll {
    		if (it.currentSwitch == "off") {
        		log.debug "Switching ON: ${it.label}"
  	       		it.on()
       	 	}
        	else
        		log.debug "Switch $it.label is already on, OFF timer canceled"
    	}
    }
}

def turnSwitchesOff() {
	targetSwitches.findAll {
    	if (it.currentSwitch == "on") {
        	log.debug "Switching OFF: ${it.label}"
  	       	it.off()
       	 }
        else
        	log.debug "Switch $it.label is already off, do nothing with it"
    }
}