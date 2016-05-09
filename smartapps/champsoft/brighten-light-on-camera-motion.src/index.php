<?php
//client id and client secret
$client = 'your-client-guid';
$secret = 'your-secret-guid';

//hardcode the full url to redirect to this file
$url = "http://xxx.xxx.xxx.xxx";

//STEP 1 - Get Access Code
if(!isset($_REQUEST['code']) && !isset($_REQUEST['access_token']))
{
	header( "Location: https://graph.api.smartthings.com/oauth/authorize?response_type=code&client_id=$client&redirect_uri=".$url."&scope=app" ) ;
}
//STEP 2 - Use Access Code to claim Access Token
else if(isset($_REQUEST['code']))
{
	$code = $_REQUEST['code'];
	$page = "https://graph.api.smartthings.com/oauth/token?grant_type=authorization_code&client_id=".$client."&client_secret=".$secret."&redirect_uri=".$url."&code=".$code."&scope=app";

	$ch = curl_init();

	curl_setopt($ch, CURLOPT_URL,            $page );
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1 );
	curl_setopt($ch, CURLOPT_POST,           0 );
	curl_setopt($ch, CURLOPT_HTTPHEADER,     array('Content-Type: application/json')); 

	$response =  json_decode(curl_exec($ch),true);

	curl_close($ch);

	if(isset($response['access_token']))
	{
		//Redirect to self with access token for step 3 for ease of bookmarking
		header( "Location: ?access_token=".$response['access_token'] ) ;
	}
	else
	{
		print "error requesting access token...";
		print_r($response);
	}

}
//Step 3 - Champsoft - Look for motion detection status
else if(isset($_REQUEST['motion_detect']))
{
	$url = "https://graph.api.smartthings.com/api/smartapps/endpoints/$client?access_token=".$_REQUEST['access_token'];
	$json = implode('', file($url));

	$theEndpoints = json_decode($json,true);
	
	foreach($theEndpoints as $k => $v)
	{

		//GET HUES
		$hueUrl = "https://graph.api.smartthings.com".$v['url']."/hues";
		$access_key = $_REQUEST['access_token'];

		$ch = curl_init($hueUrl);
		curl_setopt( $ch, CURLOPT_HTTPHEADER, array( 'Authorization: Bearer ' . $access_key ) );
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1 );
		curl_setopt($ch, CURLOPT_POST,           0 );

		$resp =  curl_exec($ch);
		curl_close($ch);

		$respData = json_decode($resp,true);

		if(count($respData) > 0) {
			//let's send the hues to the smartapp
			$hue = $respData[0];
			$newUrl = "https://graph.api.smartthings.com".$v['url']."/hues/".$hue['id']."/on?access_token=".$_REQUEST['access_token'];
			
			//open connection
			$ch = curl_init();
			
			//set the url, number of POST vars, POST data
			curl_setopt($ch,CURLOPT_URL, $newUrl);
			curl_setopt($ch,CURLOPT_POST, 0);
			//curl_setopt($ch,CURLOPT_POSTFIELDS, $fields_string);

			//execute post
			$result = curl_exec($ch);

			//close connection
			curl_close($ch);
		}
		
	}
}

//Step 4 - Lookup Endpoint and write out urls
else if(isset($_REQUEST['access_token']))
{
	$url = "https://graph.api.smartthings.com/api/smartapps/endpoints/$client?access_token=".$_REQUEST['access_token'];
	$json = implode('', file($url));

	$theEndpoints = json_decode($json,true);

	print "<html><head><style>h3{margin-left:10px;}a:hover{background-color:#c4c4c4;} a{border:1px solid black; padding:5px; margin:5px;text-decoration:none;color:black;border-radius:5px;background-color:#dcdcdc}</style></head><body>";


	print "<i>Save the above URL (access_token) for future reference.</i>";
	print " <i>Right Click on buttons to copy link address.</i>";

	foreach($theEndpoints as $k => $v)
	{

		//GET hues
		$hueUrl = "https://graph.api.smartthings.com".$v['url']."/hues";
		$access_key = $_REQUEST['access_token'];

		$ch = curl_init($hueUrl);
		curl_setopt( $ch, CURLOPT_HTTPHEADER, array( 'Authorization: Bearer ' . $access_key ) );
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1 );
		curl_setopt($ch, CURLOPT_POST,           0 );

		$resp =  curl_exec($ch);
		curl_close($ch);

		$respData = json_decode($resp,true);

		if(count($respData) > 0) print "<h2>hues</h2>";


		//let's show each of the hues
		foreach($respData as $i => $hue)
		{
			$label = $hue['label'] != "" ? $hue['label'] : "Unlabeled hue";

			print " <h3>$label</h3>";

			$onUrl = "https://graph.api.smartthings.com".$v['url']."/hues/".$hue['id']."/on?access_token=".$_REQUEST['access_token'];
			print "<a target='cmd' href='$onUrl'>On</a>";

			$offUrl = "https://graph.api.smartthings.com".$v['url']."/hues/".$hue['id']."/off?access_token=".$_REQUEST['access_token'];
			print "<a  target='cmd' href='$offUrl' value='Off'>Off</a>";

			$toggleUrl = "https://graph.api.smartthings.com".$v['url']."/hues/".$hue['id']."/toggle?access_token=".$_REQUEST['access_token'];
			print "<a target='cmd' href='$toggleUrl'>Toggle</a><BR>";
		}


		print "<BR><hr><BR>";

	}
	//all links in the html document are targeted at this iframe
	print "<iframe name='cmd' style='display:none'></iframe></body></html>";
}

?>
