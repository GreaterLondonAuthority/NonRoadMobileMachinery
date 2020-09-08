DO $$
	DECLARE
		SQLName TEXT := '106';

		wfName workflow.name%TYPE := 'Machinery Edit Form';
		wfCode workflow.code%TYPE := 'MACHINERY_EDIT_FORM';
		wfDescription workflow.description%TYPE := 'Adds workflow for machinery edit form';
		wfDisabled workflow.disabled%TYPE := false;
		wfScript workflow.script%TYPE := '
#set($LeftButtons = [])
#if(($machineryentity.exemptionStatus.toLowerCase() == "pending" || $machineryentity.exemptionStatus.toLowerCase() == "rejected") && $CurrentUser.hasAccess($PRIVILEGES_ACCEPT_PENDING_MACHINERY))
	#set($tmp=$LeftButtons.add({"id":"openAccept","label":"Accept Exemption","icon":"fa fa-clipboard-check","onClick":"machineryAdminUtils.acceptExemption","attributes":{"adminTab":"machinery","adminButton":true}}))
#end
#if($machineryentity.exemptionStatus.toLowerCase() == "pending" || $machineryentity.exemptionStatus.toLowerCase() == "accepted") && $CurrentUser.hasAccess($PRIVILEGES_REJECT_PENDING_MACHINERY))
	#set($tmp=$LeftButtons.add({"id":"openReject","label":"Reject Exemption","icon":"fa fa-remove","onClick":"machineryAdminUtils.rejectExemption","attributes":{"adminTab":"machinery","adminButton":true}}))
#end
#set($tmp=$WorkflowResult.data.put("LeftButtons",$LeftButtons))

#set($RightButtons = [])
#set($tmp=$WorkflowResult.data.put("RightButtons",$RightButtons))

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
