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
    description: "Turn on/off target switch(es) when a trigger switch is powered on/off. The target switches are delayed when the trigger switch is turning on/off.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Switch target device ON also") {
    	input "targetsOnAlso", "bool", title: "Switch devices on as well?", required: true, defaultValue: true
    }
    section("Select the target switches that you want to delay on power off") {
        input "targetSwitches", "capability.switch", title: "Target Switches", required: true, multiple: true
        input "delay", "number", title: "Switched Off Delay After Trigger is Switched Off (seconds)", required: true, 
        	defaultValue: "30"
	}
    section("Select the the trigger switch") {
        input "triggerSwitch", "capability.switch", title: "Trigger Switch", required: true
	}
    section("Trigger State that triggers the delay") {
    		input "triggerState", "bool", title: "OFF or ON?", required: true, defaultValue: false
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
    //state to indicate when the timer was initiated when the trigger device was switched on
    if(atomicState.isProcessed == null)
    	atomicState.isProcessed = false
}

def switchOnHandler(evt) {
	//If the user selects that the target should also turn on when the trigger device is switched on
    if(targetsOnAlso) {
       	turnSwitchesOn()
        log.trace "Turning switches ON."
    }
    //If user selects the trigger device's ON state to start the target's OFF timer
    if(triggerState) {
    	atomicState.isProcessed = true
    	switchOffHandler(evt)
    }
}

def switchOffHandler(evt) {
	if(evt.value == "off" && atomicState.isProcessed == true) {
    	log.trace "Attempt made to turn off switch while it may already be on a schedule. Cancelling any schedules... "
    	unschedule(turnSwitchesOff)
        atomicState.isProcessed = false
        return
    }
    
    runIn(delay, turnSwitchesOff)
   	log.trace "Starting OFF timer from event ${evt.value}"
}

def turnSwitchesOn() {
	//If there is an active off switch timer, cancel it
    unschedule(turnSwitchesOff)
    
    targetSwitches.findAll {
    	if (it.currentSwitch == "off") {
        	log.debug "Switching ON: ${it.label}"
  	    	it.on()
       	}
        else
        	log.debug "Switch $it.label is already on, Do nothing."
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