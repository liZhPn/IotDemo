<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>服务端订阅</title>
</head>
<body>
<script>
    window.onload = function () {
        console.log("执行刷新");
        location.replace("${pageContext.request.contextPath}/getMessage");
    };
</script>
</body>
</html>
