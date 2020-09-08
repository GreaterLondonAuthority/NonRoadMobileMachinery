DO $$
	DECLARE
		SQLName TEXT := '113';

		wfName workflow.name%TYPE := 'Machinery Exemption Form';
		wfCode workflow.code%TYPE := 'MACHINERY_EXEMPTION_FORM';
		wfDescription workflow.description%TYPE := 'Processes the exemption form';
		wfDisabled workflow.disabled%TYPE := false;
		wfScript workflow.script%TYPE := '
#set($ReasonId = $Request.getParameter("exemptionStatusReason"))
#set($CodeId = $Request.getParameter("exemptionStatusCode"))
#set($StartDate = $Request.getParameter("exemptionStatusDate"))
#set($MachineryId = $Request.getParameter("machineryId"))
#set($OriginalExemptionId = $Request.getParameter("originalExemptionId"))
#set($CurrentExemptionId = $Request.getParameter("exemptionId"))
#set($Action = $Request.getParameter("action").toLowerCase())

$logger.debug("Reason = $ReasonId Code = $CodeId Date = $StartDate MachineryId = $MachineryId")

#if ($Action == "accept")
  #if (!$Utils.isBlank($ReasonId) && $StartDate && $Action == "accept" && !$Utils.isBlank($CodeId))

      ## Check if TAN matches
      #set($ThisMachine = $HibernateUtils.selectFirstEntity("From MachineryEntity where id = $MachineryId"))
      #set($ThisTypeApprovalNumber = $ThisMachine.typeApprovalNumber)
      #set($ThisTanRegExp = $ThisMachine.euStage.tag)
	  #set($TANMatches = $Pattern.compile($ThisTanRegExp).matcher($ThisTypeApprovalNumber).matches())
	  #if(!$TANMatches)
      	$("#exemptionStatus").val("Check TAN");
      #end
	  #set($ReasonLookup = $HibernateUtils.selectEntities("From LookupsEntity where id = $ReasonId"))
      #if($ReasonLookup.size() > 0 && $ReasonLookup.get(0) && $ReasonLookup.get(0).tag)
          #set($TagValue = $ReasonLookup.get(0).tag)
      #else
          #set($TagValue = "1_YEAR")
      #end

      #if($TagValue)
          #set($CodeLookup = $HibernateUtils.selectEntities("From LookupsEntity where id = $CodeId"))
          #if($CodeLookup.size() > 0 && $CodeLookup.get(0) && $CodeLookup.get(0).tag)
              #set($CodeTagValue = $CodeLookup.get(0).tag)
          #else
              #set($CodeTagValue = "V_O")
          #end

		  #if($CodeTagValue)
	          #set($TagValueSplit = $Utils.split($TagValue, "_"))
	          #set($CodeTagValueSplit = $Utils.split($CodeTagValue, "_"))
    	      #if($TagValueSplit.size()==2 && $CodeTagValueSplit.size()==2)
	              #set($IncAmount = $TagValueSplit[0])
    	          #set($IncType = $TagValueSplit[1])
        	      #set($ExType = $CodeTagValueSplit[0])
            	  #set($RefCode = $CodeTagValueSplit[1])
	              #if($IncAmount && $IncType)
    	              $logger.debug("$IncAmount $IncType")
        	          #if ($IncType == "DAY")
            	          #set($CalPeriod = 7)
                	  #else
                    	  #set($CalPeriod = 1)
	                  #end
    	              #set($NewEndDate = $Utils.formatDate($Utils.addDate($StartDate, $CalPeriod, $IncAmount),"dd MMM yyyy"))
        	          $logger.debug("End date = $NewEndDate")
            	      #if($NewEndDate)
                	      $("#machineryentity\\.exemptionStatusExpiryDate").val("$NewEndDate");
	                  #end
    	          #end
	              $logger.debug("$RefCode $ExType  $CurrentExemptionId $OriginalExemptionId")
	              #if($RefCode && $ExType && ($Utils.isBlank($CurrentExemptionId) || $CurrentExemptionId != $OriginalExemptionId))
    	              #set($FormattedDate = $Utils.formatDate("$StartDate", "ddMMyyyy"))
        	          #set($SeqNum = $Utils.padLeft("$MachineryId","0",6))
            	      #set($RefNumber = "${ExType}/${FormattedDate}/${SeqNum}/${RefCode}")
                	  $logger.debug("Generated Ref Number - $RefNumber")
	                  $("#machineryentity\\.exemptionId").val("$!RefNumber");
                  #end
              #end
          #end
      #end
  #elseif(!$Utils.isBlank($CurrentExemptionId))
  	## Nothing set yet so get start date out of exemption id if posible
    #set($ExemptionIdSplit = $Utils.split($CurrentExemptionId, "/"))
    #if($ExemptionIdSplit.size() > 1 && $ExemptionIdSplit[1] && $ExemptionIdSplit[1].length() == 8)
    	$logger.debug("Existing start date = $ExemptionIdSplit[1]")
        #set($NewDate = $Utils.formatDate("$ExemptionIdSplit[1].substring(4,8)$ExemptionIdSplit[1].substring(2,4)$ExemptionIdSplit[1].substring(0,2)","dd MMM yyyy"))
        $logger.debug("Converted to $NewDate")
        #if($NewDate)
    		$("#machineryentity\\.exemptionStatusDate").val("$NewDate");
	    #end

    #else
    	$logger.debug("Unable to get date from $CurrentExemptionId")
    #end
  #end
#else
	## Assume rejecting, so only need to set start date
    #set($NewDate = $Utils.formatDate($Utils.getDate(),"dd MMM yyyy"))
    $logger.debug("Setting start date = $NewDate")
    #if($NewDate)
	    $("#machineryentity\\.exemptionStatusExpiryDate").val("$NewDate");
    	$("#machineryentity\\.exemptionStatusDate").val("$NewDate");
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
