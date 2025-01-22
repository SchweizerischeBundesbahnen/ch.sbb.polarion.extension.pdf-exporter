<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/common/jsp/about.jsp"/>

<script>
    // override help link behavior - jump to the menu item instead of external MD file page
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('a').forEach(function (element) {
            if (element.innerHTML === 'user guide') {
                const helpUrl = top.location.href.replace('/about', '/user-guide');
                element.href = helpUrl;
                element.target = "";
                element.onclick = function(event) {
                    event.preventDefault();
                    parent.document.location = helpUrl;
                };
            }
        });
    });
</script>
