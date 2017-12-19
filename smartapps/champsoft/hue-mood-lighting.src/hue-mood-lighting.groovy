/**
 *  Copyright 2015 SmartThings
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
 *  Hue Mood Lighting
 *
 *  Author: SmartThings, Champsoft
 *  *
 *  Date: 2014-02-21
 *
 * Edited by Champsoft 2016-10-29
 * Added the ability for each day to have its own different predefined color and saturation
 */
definition(
    name: "Hue Mood Lighting",
    namespace: "champsoft",
    author: "SmartThings, Champsoft",
    description: "Sets the colors and brightness level of your Philips Hue lights to match your mood.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png"
)

preferences {
	page(name: "mainPage", title: "Adjust the color of your Hue lights to match your mood.", install: true, uninstall: true)
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
    page(name: "colorSelectPage", title: "Select color of the day.")
}

def colorSelectPage() {
	dynamicPage(name: "colorSelectPage") {
    	section {
    		days.each { day ->
        		input "$day", "enum", title: "Select a color for $day", required: false, 
                	options: ["Red", "Yellow", "Pink", "Green", "Orange", "Blue", "Purple"]
                input "saturation$day", "number", title: "Saturation Level for $day?", description: "Between 1 and 100", required: false
            }
        }
    }
}

def mainPage() {
	dynamicPage(name: "mainPage") {
		section("Enable app...") {
    		input "appEnabled", "bool", title: "Enable this app", required: true, defaultValue: true
    	}
		def anythingSet = anythingSet()
		if (anythingSet) {
			section("Set the lighting mood when..."){
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
				ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
				ifSet "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
				ifSet "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
				ifSet "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
				ifSet "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
				ifSet "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
				ifSet "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
				ifSet "timeOfDay", "time", title: "At a Scheduled Time", required: false
			}
		}
		section(anythingSet ? "Select additional mood lighting triggers" : "Set the lighting mood when...", hideable: anythingSet, hidden: true){
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
			ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
			ifUnset "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
			ifUnset "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
			ifUnset "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
			ifUnset "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
			ifUnset "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
			ifUnset "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true
			ifUnset "timeOfDay", "time", title: "At a Scheduled Time", required: false
		}
		section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
		}
        section("Select days of the week") {
        	input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
            href "colorSelectPage", title: "Select color of the day.", description: colorState() ? "Tap to show": "Tap to set", state: colorState() ? "complete" : "",
            	required: days? true : false
        }
		section("Choose light effects...")
			{
				input "lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
				//input "saturationLevel", "number", title: "Saturation Level?", description: "Between 1 and 100", required: false
                
            }
		section("More options", hideable: true, hidden: true) {
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}
private anythingSet() {
	for (name in ["motion","contact","contactClosed","acceleration","mySwitch","mySwitchOff","arrivalPresence","departurePresence","smoke","water","button1","triggerModes","timeOfDay"]) {
		if (settings[name]) {
			return true
		}
	}
	return false
}

private colorState() {
	//Check to see if all days are included
    //The result determines the description and state of the color selection href
	def result = false
    if(days) {
		for (dayStr in days) {
			if (settings[dayStr]) {
				result = true
			}
			else {
				result = false
				break
			}
		}
    }
	return result
}
        

private ifUnset(Map options, String name, String capability) {
	if (!settings[name]) {
		input(options, name, capability)
	}
}

private ifSet(Map options, String name, String capability) {
	if (settings[name]) {
		input(options, name, capability)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	try{
    	unschedule()
	}
    catch(all) {
    	log.error("unschedule() method is called, but there is nothing to unschedle\n${all}")
    }
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
	subscribe(button1, "button.pushed", eventHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}

def eventHandler(evt=null) {
	log.trace "Executing Mood Lighting"
	if (allOk) {
		log.trace "allOk"
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					takeAction(evt)
				}
			}
			else {
				takeAction(evt)
			}
		}
		else {
			log.debug "Not taking action because it was already taken today"
		}
	}
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

def scheduledTimeHandler() {
	log.trace "scheduledTimeHandler()"
	eventHandler()
}

def appTouchHandler(evt) {
    eventHandler()
}

private takeAction(evt) {

	if (frequency || oncePerDay) {
		state[frequencyKey(evt)] = now()
	}

	def hueColor = 0
	def saturation = 100

    log.trace "Today is $state.today and today's color is settings[state.today]"

	switch(settings[state.today]) {
		case "White":
			hueColor = 52
			saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : 80 
			break;
		case "Blue":
			hueColor = 70
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
			break;
		case "Green":
			hueColor = 39
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
            break;
		case "Yellow":
			hueColor = 25
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
			break;
		case "Orange":
			hueColor = 10
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
			break;
		case "Purple":
			hueColor = 75
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
			break;
		case "Pink":
			hueColor = 83
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
			break;
		case "Red":
			hueColor = 100
            saturation = settings["saturation" + state.today] ? settings["saturation" + state.today] : saturation
			break;
        default:
        	//Daylight
        	hueColor = 53
			saturation = 91
	}

	if(appEnabled) {
		state.previous = [:]

		hues.each {
			state.previous[it.id] = [
				"switch": it.currentValue("switch"),
				"level" : it.currentValue("level"),
				"hue": it.currentValue("hue"),
				"saturation": it.currentValue("saturation")
			]
		}

		log.debug "current values = $state.previous"

		def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
		log.debug "new value = $newValue"

		hues*.setColor(newValue)
    }
}

private frequencyKey(evt) {
	"lastActionTimeStamp"
}

private dayString(Date date) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	df.format(date)
}


private oncePerDayOk(Long lastTime) {
	def result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
	log.trace "oncePerDayOk = $result - $lastTime"
	result
}

// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
        //Added this state variable to capture the day (in words)
        //It will enable the setting of a color for the day
        state.today = day
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
// TODO - End Centralize