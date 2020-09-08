/**
 * An Admin utils namespace for showing the admin utils common to cases and meetings
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("dateUtils");
    psApp.dateUtils = (function () {

            // Compares start and end date
            // Returns true if both dates are in correct format
            // and date1 is less than or equal to date2
            var compareStartEndDates = function (dateName1, dateName2, dateFormat) {
                var returnValue = false;
                var checkDateElement1 = $(dateName1);
                var checkDateElement2 = $(dateName2);
                var checkDate1 = this.parseDate(checkDateElement1.val(), dateFormat);
                var checkDate2 = this.parseDate(checkDateElement2.val(), dateFormat);

                if (!checkDate1 || !checkDate2) {
                    alert("Please enter dates using the dd mmm yyyy format. eg. 10 Nov 2019");
                    !checkDate1 ? checkDateElement1.focus() : checkDateElement2.focus();
                }
                else {
                    returnValue = checkDate1 <= checkDate2;
                    if (!returnValue)
                        alert("Please ensure the start and end dates are in the correct order");
                }

                return returnValue;
            };

            // Returns date object if valid otherwise null
            var parseDate = function(dateValue, dateFormat) {
                return kendo.parseDate(dateValue, dateFormat);
            };

            return {
                compareStartEndDates:compareStartEndDates,
                parseDate:parseDate
            };
    })();
}
