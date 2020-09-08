DO $$
	DECLARE
		SQLName TEXT := '105';

		wfName workflow.name%TYPE := 'Register Machinery Form';
		wfCode workflow.code%TYPE := 'MACHINERY_REGISTER_FORM';
		wfDescription workflow.description%TYPE := 'Processes exemption field settings for the machinery registration form';
		wfDisabled workflow.disabled%TYPE := false;
		wfScript workflow.script%TYPE := '
#if($FormParams.get("PROCESS_FIELD") == "EUSTAGE")

	## Work out if an exemption is required

    ## This is the date the new exemption policy comes into affect
    #set($ChangeOverDate = $Utils.parseDate("1 Sep 2020"))
    #set($BeforeChangeOver = $Utils.getDate().before($ChangeOverDate))

    ## This is the name of the stage the user has selected
    #set($EuStage = $Utils.trim($FormParams.get("euStageName"), " ").toLowerCase())

    ## This is true if the site is not in the greater london zone (ie in tighter control zone)
    #set($InTightZone = $Utils.trim($FormParams.get("siteZone"), " ").toLowerCase() != "greater london")

	## Choose a list of stages depending on date and zone
    #if($BeforeChangeOver)
    	#if($InTightZone)
        	#set($CheckList = "eu stage i,eu stage ii,eu stage iiia")
        #else
        	#set($CheckList = "eu stage i,eu stage ii")
        #end
    #else
    	#if($InTightZone)
        	#set($CheckList = "eu stage i,eu stage ii,eu stage iiia,eu stage iiib")
        #else
        	#set($CheckList = "eu stage i,eu stage ii,eu stage iiia")
        #end
    #end
    ## See if the selected stage needs an exemption

    #set($ShowExemption=$Utils.splitToList("$CheckList",",").contains("$EuStage"))

    #if($ShowExemption)
    	## Show exemption entry section on the page
	  	#macroExemptionSection(true)
  	#else

		## Hide the exemption entry sections
  		#macroExemptionSection(false)
		#macroFileSection(false)
  	#end

#elseif($FormParams.get("PROCESS_FIELD") == "EXEMPTIONREASON")
    #set($ernValue = $Utils.trim($FormParams.get("exemptionReasonName"), " "))
	#if ($ernValue.equals("retrofit"))
    	#macroExemptionReasonText(false)
        #macroExemptionId(false)
	  	#macroRetrofitId(true)
	  	#macroFileSection(true)
   	  	#macroRetrofitModel(true)

	#elseif ($ernValue.equals("existing exemption"))
        #macroExemptionId(true)
		#macroExemptionReasonText(false)
	  	#macroRetrofitId(false)
	  	#macroFileSection(false)
	  	#macroRetrofitModel(false)

	#elseif ($ernValue.equals("other"))
        #macroExemptionId(false)
		#macroExemptionReasonText(true)
	  	#macroRetrofitId(false)
	  	#macroFileSection(true)
	  	#macroRetrofitModel(false)

	#else
    	#set($ShowReason = !$Utils.isBlank($ernValue))
		#macroExemptionReasonText($ShowReason)
        #macroExemptionId(false)
	  	#macroRetrofitId(false)
	  	#macroFileSection(false)
	  	#macroRetrofitModel(false)
  #end

  machineryAdminUtils.checkForOtherRetrofitModel();

#elseif($FormParams.get("PROCESS_FIELD") == "SUBMIT")
	function validateMRF() {
        #set($checkTAN = false)
        #set($exemptionStatus = $FormParams.get("exemptionStatus"))

        #if($Utils.isBlank($FormParams.get("exemptionReason")))
        	## No reason needed
    	    #if ($Utils.isYes($FormParams.get("tanMatch")))
        	    ## Good TAN
                #set($exemptionStatus = "OK")
            #else
            	#set($exemptionStatus = "Check TAN")
            	#set($checkTAN = true)
            #end
	        $("#machineryentity\\.exemptionStatus").val("$!exemptionStatus");

		#elseif($Utils.isBlank($exemptionStatus) || $exemptionStatus == "Pending")
            #set($exemptionStatus = "Pending")
	        $("#machineryentity\\.exemptionStatus").val("$!exemptionStatus");
            #set($checkTAN = !$Utils.isYes($FormParams.get("tanMatch")))

		#elseif($exemptionStatus == "OK" || $exemptionStatus == "Accepted" || $exemptionStatus == "Check TAN")
        	## see if TAN still matches
    	    #if ($Utils.isYes($FormParams.get("tanMatch")))
        	    ## Good TAN
        	    #if($Utils.isBlank($FormParams.get("exemptionReason")))
                    #set($exemptionStatus = "OK")
                #else
                    #set($exemptionStatus = "Accepted")
                #end
            #else
            	#set($exemptionStatus = "Check TAN")
            	#set($checkTAN = true)
            #end
	        $("#machineryentity\\.exemptionStatus").val("$!exemptionStatus");

        #end

        #if ($checkTAN)
	        var returnValue = confirm("The Type Approval Number is not in the correct format.\r\nAre you sure you want to submit this data?");
        #else
        	var returnValue = true;
        #end

        if (returnValue) {
            var message = "Machinery Updated\r\nCompliance Status is $!exemptionStatus";
            var messageType = "";
            #if ($exemptionStatus.toLowerCase() == "ok")
            	messageType = "success";
            #elseif ($exemptionStatus.toLowerCase() == "pending" || $exemptionStatus.toLowerCase() == "check tan")
                messageType = "warning";
            #else
                messageType = "error";
            #end
            PivotalUtils.showNotification(message, messageType);
        }
        return returnValue;
    }
    validateMRF();
#end

#macro(macroExemptionSection $showElement)

    #if ($showElement)
      	$("#exemptionSection").show();
      	$("#machineryentity\\.exemptionReason").attr("required", "required");
		$("#machineryentity\\.exemptionReason").attr("validationmessage", "#I18N("forms.generic.required.message")");
    #else
	    $("#exemptionSection").hide();
    	$("#machineryentity\\.exemptionReason").attr("required", false);
    	$("#machineryentity\\.exemptionReason").attr("validationmessage", "");
    	$("#machineryentity\\.exemptionReason").data("kendoDropDownList").value("");
        #macroExemptionReasonText(false)
        #macroRetrofitId(false)
        #macroRetrofitModel(false)
        #macroRetrofitModelOther(false)
    #end
#end

#macro(macroExemptionReasonText $showElement)

    #if ($showElement)
        $("#machineryentity\\.exemptionReasonText").closest(".input-line").show();
        $("#machineryentity\\.exemptionReasonText").attr("required", "required");
        $("#machineryentity\\.exemptionReasonText").attr("validationmessage", "#I18N("forms.generic.required.message")");
        ##$("#machineryentity\\.exemptionReasonText").val("");
    #else
        $("#machineryentity\\.exemptionReasonText").closest(".input-line").hide();
        $("#machineryentity\\.exemptionReasonText").attr("required", false);
        $("#machineryentity\\.exemptionReasonText").attr("validationmessage", "");
        $("#machineryentity\\.exemptionReasonText").val("");
    #end
#end

#macro(macroRetrofitId $showElement)

    #if ($showElement)
		$("#machineryentity\\.retrofitId").closest(".input-line").show();
        $("#machineryentity\\.retrofitId").attr("required", "required");
		$("#machineryentity\\.retrofitId").attr("validationmessage", "#I18N("forms.generic.required.message")");
    #else
		$("#machineryentity\\.retrofitId").closest(".input-line").hide();
		$("#machineryentity\\.retrofitId").val("");
        $("#machineryentity\\.retrofitId").attr("required", false);
		$("#machineryentity\\.retrofitId").attr("validationmessage", "");
    #end
#end

#macro(macroRetrofitModel $showElement)

    #if ($showElement)
	  	$("#machineryentity\\.retrofitModel").closest(".input-line").show();
        $("#machineryentity\\.retrofitModel").attr("required", "required");
		$("#machineryentity\\.retrofitModel").attr("validationmessage", "#I18N("forms.generic.required.message")");
    #else
		$("#machineryentity\\.retrofitModel").closest(".input-line").hide();
      	$("#machineryentity\\.retrofitModel").data("kendoDropDownList").value("");
        $("#machineryentity\\.retrofitModel").attr("required", false);
		$("#machineryentity\\.retrofitModel").attr("validationmessage", "");
    #end
#end

#macro(macroExemptionId $showElement)

    #if ($showElement)
	  	$("#machineryentity\\.exemptionId").closest(".input-line").show();
        $("#machineryentity\\.exemptionId").attr("required", "required");
		$("#machineryentity\\.exemptionId").attr("validationmessage", "#I18N("forms.generic.required.message")");
    #else
		$("#machineryentity\\.exemptionId").closest(".input-line").hide();
      	$("#machineryentity\\.exemptionId").val("");
        $("#machineryentity\\.exemptionId").attr("required", false);
		$("#machineryentity\\.exemptionId").attr("validationmessage", "");
    #end
#end

#macro(macroRetrofitModelOther $showElement)

    #if ($showElement)
	  	$("#machineryentity\\.retrofitModelOther").closest(".input-line").show();
        $("#machineryentity\\.retrofitModelOther").attr("required", "required");
		$("#machineryentity\\.retrofitModelOther").attr("validationmessage", "#I18N("forms.generic.required.message")");
    #else
		$("#machineryentity\\.retrofitModelOther").closest(".input-line").hide();
      	$("#machineryentity\\.retrofitModelOther").val("");
        $("#machineryentity\\.retrofitModelOther").attr("required", false);
		$("#machineryentity\\.retrofitModelOther").attr("validationmessage", "");
    #end
#end

#macro(macroFileSection $showElement)

    #if ($showElement)
		$("#exemptionFilesSection").show();
		$("#UseFiles").val("true");
    #else
		$("#exemptionFilesSection").hide();
		$("#UseFiles").val("false");
    #end
#end
		';

	BEGIN

		EXECUTE
      		'INSERT INTO nrmm.workflow (name, description, disabled, code, script) select $1, $2, $3, $4, $5 WHERE not exists (SELECT 1 FROM nrmm.workflow where code=$4)'
		USING wfName, wfDescription, wfDisabled, wfCode, '';

		EXECUTE
      		'UPDATE nrmm.workflow SET name = $1, description = $2, disabled = $3, code = $4, script = $5 where code=$4'
		USING wfName, wfDescription, wfDisabled, wfCode, wfScript;

	EXCEPTION
		WHEN OTHERS THEN
			RAISE NOTICE 'Error in database modification %', SQLName;
	END
$$;
