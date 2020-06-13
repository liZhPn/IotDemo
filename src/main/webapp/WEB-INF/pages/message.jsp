<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>服务端订阅</title>
    <style>
        table {
            width: 100%;
            margin-bottom: 15px;
        }

        caption{
            font-size: 20px;
        }

        td, th {
            border: 1px solid #A6A6A6;
            /*设置边框圆角*/
            border-radius: 5px;
            padding-left: 0px;
        }
    </style>

</head>
<body>
<div>
    <span>
        前往<a href="${pageContext.request.contextPath}/static/deviceManager.jsp" target="_blank">设备云端管理页面</a>
    </span>
</div>
<div>
    <div>
        <table>
            <caption>属性上报记录</caption>
            <thead>
            <tr>
                <th>Topic</th>
                <th>MessageID</th>
                <th>产品标识</th>
                <th>设备名称</th>
                <th>属性值</th>
                <th>最后通信时间</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${property}" var="a">
                <tr>
                    <td>${a.getString("topic")}</td>
                    <td>${a.getString("messageId")}</td>
                    <td>${a.getString("productKey")}</td>
                    <td>${a.getString("deviceName")}</td>
                    <td>${a.getJSONObject("items").getJSONObject("temperature").getInteger("value")}</td>
                    <td>${a.getJSONObject("items").getJSONObject("temperature").getDate("time")}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <div>
        <table>
            <caption>设备状态变化</caption>
            <thead>
            <tr>
                <th>Topic</th>
                <th>MessageID</th>
                <th>产品标识</th>
                <th>设备名称</th>
                <th>设备状态</th>
                <th>最后通信时间</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${status}" var="a">
                <tr>
                    <td>${a.getString("topic")}</td>
                    <td>${a.getString("messageId")}</td>
                    <td>${a.getString("productKey")}</td>
                    <td>${a.getString("deviceName")}</td>
                    <td>${a.getString("status")}</td>
                    <td>${a.getDate("lastTime")}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <div>
        <table>
            <caption>设备事件上报</caption>
            <thead>
            <tr>
                <th>Topic</th>
                <th>MessageID</th>
                <th>产品标识</th>
                <th>设备名称</th>
                <th>事件的标识符</th>
                <th>事件类型</th>
                <th>事件的输出参数</th>
                <th>事件上报时间</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${event}" var="a">
                <tr>
                    <td>${a.getString("topic")}</td>
                    <td>${a.getString("messageId")}</td>
                    <td>${a.getString("productKey")}</td>
                    <td>${a.getString("deviceName")}</td>
                    <td>${a.getString("identifier")}</td>
                    <td>${a.getString("type")}</td>
                    <td>${a.getJSONObject("value")}</td>
                    <td>${a.getDate("time")}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <div>
        <table>
            <caption>服务调用返回结果</caption>
            <thead>
            <tr>
                <th>Topic</th>
                <th>MessageID</th>
                <th>产品标识</th>
                <th>设备名称</th>
                <th>结果来源</th>
                <th>结果数据</th>
                <th>服务调用时间</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${service}" var="a">
                <tr>
                    <td>${a.getString("topic")}</td>
                    <td>${a.getString("messageId")}</td>
                    <td>${a.getString("productKey")}</td>
                    <td>${a.getString("deviceName")}</td>
                    <td>${a.getString("source")}</td>
                    <td>${a.getString("data")}</td>
                    <td>${a.getDate("gmtCreate")}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<script>
    window.setInterval(function () {
        console.log("执行刷新");
        location.replace("${pageContext.request.contextPath}/getMessage");
    }, 90000);
</script>
</body>
</html>
