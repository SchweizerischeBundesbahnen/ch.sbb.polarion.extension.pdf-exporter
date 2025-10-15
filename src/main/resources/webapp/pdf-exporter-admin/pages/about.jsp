<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/common/jsp/about.jsp"/>

<script>
    (function () {
        // override help and disclaimer links behavior - jump to the menu item instead of external MD file page
        document.addEventListener('DOMContentLoaded', function () {
            document.querySelectorAll('a').forEach(function (element) {
                if (element.innerHTML === 'user guide') {
                    replaceAnchorUrl(element, '/user-guide');
                }
                if (element.innerHTML === 'disclaimer') {
                    replaceAnchorUrl(element, '/disclaimer');
                }
            });
        });

        function replaceAnchorUrl(anchor, targetPath) {
            const url = top.location.href.replace('/about', targetPath);
            anchor.href = url;
            anchor.target = "";
            anchor.onclick = function(event) {
                event.preventDefault();
                parent.document.location = url;
            };
        }
    })();
</script>
