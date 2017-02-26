<?php
/****************************************
*		Server of Android IM Application
*
*		Author: sanchita
* 		Email: santra.sanchita13@gmail.com
*		Editor: sanchita
*		Email: santra.sanchita13@gmail.com
* 		Date: Apr, 27, 2015   
* 	
*		Supported actions: 
*			1.  authenticateUser
*			    if user is authentiated return device list
* 		    
*			2.  signUpUser
* 		
*			3.  addNewDevice
* 		
* 			4.  responseOfDeviceReqs
*
*			5.  testWebAPI
*************************************/


//TODO:  show error off

require_once("mysql.class.php");

$dbHost = "localhost";
$dbUsername = "977253";
$dbPassword = "santra13";
$dbName = "977253";


$db = new MySQL($dbHost,$dbUsername,$dbPassword,$dbName);

// if operation is failed by unknown reason
define("FAILED", 0);

define("SUCCESSFUL", 1);
// when  signing up, if username is already taken, return this error
define("SIGN_UP_USERNAME_CRASHED", 2);  
// when add new device request, if device is not found, return this error 
define("ADD_NEW_USERNAME_NOT_FOUND", 2);

// TIME_INTERVAL_FOR_USER_STATUS: if last authentication time of user is older 
// than NOW - TIME_INTERVAL_FOR_USER_STATUS, then user is considered offline
define("TIME_INTERVAL_FOR_USER_STATUS", 60);

define("USER_APPROVED", 1);
define("USER_UNAPPROVED", 0);


$username = (isset($_REQUEST['username']) && count($_REQUEST['username']) > 0) 
							? $_REQUEST['username'] 
							: NULL;
$password = isset($_REQUEST['password']) ? md5($_REQUEST['password']) : NULL;
$port = isset($_REQUEST['port']) ? $_REQUEST['port'] : NULL;

$action = isset($_REQUEST['action']) ? $_REQUEST['action'] : NULL;
if ($action == "testWebAPI")
{
	if ($db->testconnection()){
	echo SUCCESSFUL;
	exit;
	}else{
	echo FAILED;
	exit;
	}
}

if ($username == NULL || $password == NULL)	 
{
	echo FAILED;
	exit;
}

$out = NULL;

error_log($action."\r\n", 3, "error.log");
switch($action) 
{
	
	case "authenticateUser":
		
		
		if ($userId = authenticateUser($db, $username, $password)) 
		{					
			
			// providerId and requestId is Id of  a device pair,
			// providerId is the Id of making first device request
			// requestId is the Id of the device approved the device request made by providerId
			
			// fetching devices, 
			// left join expression is a bit different, 
			//		it is required to fetch the device, not the users itself
			
			$sql = "select u.Id, u.username, (NOW()-u.authenticationTime) as authenticateTimeDifference, u.IP, 
										f.providerId, f.requestId, f.status, u.port 
							from devices f
							left join users u on 
										u.Id = if ( f.providerId = ".$userId.", f.requestId, f.providerId ) 
							where (f.providerId = ".$userId." and f.status=".USER_APPROVED.")  or 
										 f.requestId = ".$userId." ";
										 
			//$sqlmessage = "SELECT * FROM `messages` WHERE `touid` = ".$userId." AND `read` = 0 LIMIT 0, 30 ";
			
			$sqlmessage = "SELECT m.id, m.fromuid, m.touid, m.sentdt, m.read, m.readdt, m.messagetext, u.username from messages m \n"
    . "left join users u on u.Id = m.fromuid WHERE `touid` = ".$userId." AND `read` = 0 LIMIT 0, 30 ";
			
	
			if ($result = $db->query($sql))			
			{
					$out .= "<data>"; 
					$out .= "<user userKey='".$userId."' />";
					while ($row = $db->fetchObject($result))
					{
						$status = "offline";
						if (((int)$row->status) == USER_UNAPPROVED)
						{
							$status = "unApproved";
						}
						else if (((int)$row->authenticateTimeDifference) < TIME_INTERVAL_FOR_USER_STATUS)
						{
							$status = "online";
							 
						}
						$out .= "<device  username = '".$row->username."'  status='".$status."' IP='".$row->IP."' userKey = '".$row->Id."'  port='".$row->port."'/>";
												
												// to increase security, we need to change userKey periodically and pay more attention
												// receiving message and sending message 
						
					}
						if ($resultmessage = $db->query($sqlmessage))			
							{
							while ($rowmessage = $db->fetchObject($resultmessage))
								{
								$out .= "<message  from='".$rowmessage->username."'  sendt='".$rowmessage->sentdt."' text='".$rowmessage->messagetext."' />";
								$sqlendmsg = "UPDATE `messages` SET `read` = 1, `readdt` = '".DATE("Y-m-d H:i")."' WHERE `messages`.`id` = ".$rowmessage->id.";";
								$db->query($sqlendmsg);
								}
							}
					$out .= "</data>";
			}
			else
			{
				$out = FAILED;
			}			
		}
		else
		{
				// exit application if not authenticated user
				$out = FAILED;
		}
		
	
	
	break;
	
	case "signUpUser":
		if (isset($_REQUEST['email']))
		{
			 $email = $_REQUEST['email'];		
			 	
			 $sql = "select Id from  users 
			 				where username = '".$username."' limit 1";
			 
		
			 				
			 if ($result = $db->query($sql))
			 {
			 		if ($db->numRows($result) == 0) 
			 		{
			 				$sql = "insert into users(username, password, email)
			 					values ('".$username."', '".$password."', '".$email."') ";		 					
						 					
			 					error_log("$sql", 3 , "error_log");
							if ($db->query($sql))	
							{
							 		$out = SUCCESSFUL;
							}				
							else {
									$out = FAILED;
							}				 			
			 		}
			 		else
			 		{
			 			$out = SIGN_UP_USERNAME_CRASHED;
			 		}
			 }				 	 	
		}
		else
		{
			$out = FAILED;
		}	
	break;
	
	case "sendMessage":
	if ($userId = authenticateUser($db, $username, $password)) 
		{	
		if (isset($_REQUEST['to']))
		{
			 $tousername = $_REQUEST['to'];	
			 $message = $_REQUEST['message'];	
				
			 $sqlto = "select Id from  users where username = '".$tousername."' limit 1";
			 
			 
		
					if ($resultto = $db->query($sqlto))			
					{
						while ($rowto = $db->fetchObject($resultto))
						{
							$uto = $rowto->Id;
						}
						$sql22 = "INSERT INTO `messages` (`fromuid`, `touid`, `sentdt`, `messagetext`) VALUES ('".$userId."', '".$uto."', '".DATE("Y-m-d H:i")."', '".$message."');";						
						 					
			 					error_log("$sql22", 3 , "error_log");
							if ($db->query($sql22))	
							{
							 		$out = SUCCESSFUL;
							}				
							else {
									$out = FAILED;
							}				 		
						$resultto = NULL;
					}	
			 				 	 	
		$sqlto = NULL;
		}
		}
		else
		{
			$out = FAILED;
		}	
	break;
	
	case "addNewDevice":
		$userId = authenticateUser($db, $username, $password);
		if ($userId != NULL)
		{
			
			if (isset($_REQUEST['deviceUserName']))			
			{				
				 $deviceUserName = $_REQUEST['deviceUserName'];
				 
				 $sql = "select Id from users 
				 				 where username='".$deviceUserName."' 
				 				 limit 1";
				 if ($result = $db->query($sql))
				 {
				 		if ($row = $db->fetchObject($result))
				 		{
				 			 $requestId = $row->Id;
				 			 
				 			 if ($row->Id != $userId)
				 			 {
				 			 		 $sql = "insert into devices(providerId, requestId, status)
				 				  		 values(".$userId.", ".$requestId.", ".USER_UNAPPROVED.")";
							 
									 if ($db->query($sql))
									 {
									 		$out = SUCCESSFUL;
									 }
									 else
									 {
									 		$out = FAILED;
									 }
							}
							else
							{
								$out = FAILED;  // user add itself as a device
							} 		 				 				  		 
				 		}
				 		else
				 		{
				 			$out = FAILED;			 			
				 		}
				 }				 				 
				 else
				 {
				 		$out = FAILED;
				 }				
			}
			else
			{
					$out = FAILED;
			} 			
		}
		else
		{
			$out = FAILED;
		}	
	break;
	
	case "responseOfDeviceReqs":
		$userId = authenticateUser($db, $username, $password);
		if ($userId != NULL)
		{
			$sqlApprove = NULL;
			$sqlDiscard = NULL;
			if (isset($_REQUEST['approvedDevices']))
			{
				  $deviceNames = split(",", $_REQUEST['approvedDevices']);
				  $deviceCount = count($deviceNames);
				  $deviceNamesQueryPart = NULL;
				  for ($i = 0; $i < $deviceCount; $i++)
				  {
				  	if (strlen($deviceNames[$i]) > 0)
				  	{
				  		if ($i > 0 )
				  		{
				  			$deviceNamesQueryPart .= ",";
				  		}
				  		
				  		$deviceNamesQueryPart .= "'".$deviceNames[$i]."'";
				  		
				  	}			  	
				  	
				  }
				  if ($deviceNamesQueryPart != NULL)
				  {
				  	$sqlApprove = "update devices set status = ".USER_APPROVED."
				  					where requestId = ".$userId." and 
				  								providerId in (select Id from users where username in (".$deviceNamesQueryPart."));
				  				";		
				  }
				  				  
			}
			if (isset($_REQUEST['discardedDevices']))
			{
					$deviceNames = split(",", $_REQUEST['discardedDevices']);
				  $deviceCount = count($deviceNames);
				  $deviceNamesQueryPart = NULL;
				  for ($i = 0; $i < $deviceCount; $i++)
				  {
				  	if (strlen($deviceNames[$i]) > 0)
				  	{
				  		if ($i > 0 )
				  		{
				  			$deviceNamesQueryPart .= ",";
				  		}
				  		
				  		$deviceNamesQueryPart .= "'".$deviceNames[$i]."'";
				  		
				  	}				  	
				  }
				  if ($deviceNamesQueryPart != NULL)
				  {
				  	$sqlDiscard = "delete from devices 
				  						where requestId = ".$userId." and 
				  									providerId in (select Id from users where username in (".$deviceNamesQueryPart."));
				  							";
				  }						
			}
			if (  ($sqlApprove != NULL ? $db->query($sqlApprove) : true) &&
						($sqlDiscard != NULL ? $db->query($sqlDiscard) : true) 
			   )
			{
				$out = SUCCESSFUL;
			}
			else
			{
				$out = FAILED;
			}		
		}
		else
		{
			$out = FAILED;
		}
	break;
	
	default:
		$out = FAILED;		
		break;	
}

echo $out;



///////////////////////////////////////////////////////////////
function authenticateUser($db, $username, $password)
{
	
	$sql22 = "select * from users 
					where username = '".$username."' and password = '".$password."' 
					limit 1";
	
	$out = NULL;
	if ($result22 = $db->query($sql22))
	{
		if ($row22 = $db->fetchObject($result22))
		{
				$out = $row22->Id;
				
				$sql22 = "update users set authenticationTime = NOW(), 
																 IP = '".$_SERVER["REMOTE_ADDR"]."' ,
																 port = 15145 
								where Id = ".$row22->Id."
								limit 1";
				
				$db->query($sql22);				
								
								
		}		
	}
	
	return $out;
}

?>

