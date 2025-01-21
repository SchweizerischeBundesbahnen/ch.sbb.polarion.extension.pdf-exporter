<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/common/jsp/about.jsp"/>

<script>
    // override help link behavior - jump to the menu item instead of external MD file page
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('a').forEach(function (element) {
            if (element.innerHTML === 'help page') {
                element.target = "";
                element.href = "";
                element.onclick = function(event) {
                    event.preventDefault();
                    parent.document.location = top.location.href.replace('/about', '/help')
                };
            }
        });
    });
</script>
