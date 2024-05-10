<link rel="stylesheet" href="${request.contextPath}/plugin/${className}/node_modules/select2/dist/css/select2.min.css" />
<link rel="stylesheet" href="${request.contextPath}/plugin/${className}/node_modules/selectize/dist/css/selectize.default.css" />
<script type="text/javascript" src="${request.contextPath}/plugin/${className}/node_modules/select2/dist/js/select2.min.js"></script>
<script type="text/javascript" src="${request.contextPath}/plugin/${className}/node_modules/selectize/dist/js/standalone/selectize.min.js"></script>
<script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/selectize-infinite-scroll.js"></script>

<style>
    .select2-container--default .select2-results__option[aria-selected=true] {
        display: none;
    }
</style>
<select class="chosen-select" id="${name!}-filter" name="${name!}" multiple style="width: 400px; display: none;">
    <option value=""></option>
    <#list options as option>
        <option value="${option.value!?html}" <#if values?? && values?seq_contains(option.value!)>selected</#if>>${option.label!?html}</option>
    </#list>
</select>

<script type="text/javascript">
$(document).ready(function() {
    $('.chosen-select').select2({
        placeholder: "Choose your selection!",
        allowClear: true
    });
});
</script>