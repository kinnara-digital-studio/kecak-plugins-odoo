<div>
    <#if locale! != ''>
        <script type="text/javascript" src="${request.contextPath}/js/jquery/ui/i18n/jquery.ui.datepicker-${locale}.js"></script>
    </#if>
    <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/js/jquery.placeholder.min.js"></script>
    <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/css/datePicker.css" />
    <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/css/jquery-ui-timepicker-addon.css" />
    <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/js/jquery-ui-timepicker-addon.js"></script>
    <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/js/jquery.custom.datepicker.js"></script>

    <div style="float: left;padding-right: 10px;">
        <input id="${name}-from" name="${name}-from" type="text" size="${properties.size!}" value="${value!?html}" class="${elementParamName!} datetimepicker" placeholder="From : ${label}" value="${valueFrom!}" readonly}"/>
    </div>

    <#if (singleValue!'false') != 'true' >
        <div style="float: left;padding-right: 10px;">
            <input id="${name}-to" name="${name}-to" type="text" size="${properties.size!}" value="${value!?html}" class="${elementParamName!} datetimepicker" placeholder="To : ${label}" value="${valueTo!}" readonly/>
        </div>
    </#if>

    <script type="text/javascript">
        $(document).ready(function() {
            $("input[id^='${name}-'].datetimepicker").each(function(i, e) {
                let config = {
                     showOn: "focus",
                     buttonImage: "${request.contextPath}/css/images/calendar.png",
                     buttonImageOnly: true,
                     changeMonth: true,
                     changeYear: true,
                     timeInput: true

                     <#if properties.format24hr! == ''>
                        ,timeFormat: "hh:mm tt"
                     </#if>

                     ,dateFormat: "yy-mm-dd"

                     <#if properties.yearRange! != ''>
                        ,yearRange: "${properties.yearRange}"
                     </#if>

                     <#if properties.startDateFieldId! != ''>
                        ,startDateFieldId: "${properties.startDateFieldId}"
                     </#if>

                     <#if properties.endDateFieldId! != ''>
                        ,endDateFieldId: "${properties.endDateFieldId}"
                     </#if>

                     <#if properties.currentDateAs! != ''>
                        ,currentDateAs: "${properties.currentDateAs}"
                     </#if>

                     <#if properties.datePickerType! != ''>
                        ,datePickerType: "${properties.datePickerType}"
                     </#if>

                     <#if properties.firstday! != ''>
                        ,firstDay: "${properties.firstday}"
                     </#if>
                };

                $(e).cdatepicker(config);
            });
        });
    </script>
</div>
