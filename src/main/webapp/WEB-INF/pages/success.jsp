<%--
  Created by IntelliJ IDEA.
  User: lenovo
  Date: 2020/5/1
  Time: 23:17
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>设备管理结果</title>

    <style>
        table {
            border: 1px solid;
            margin: 10px;
            width: 80%;
        }

        th {
            text-align: center;
            border: 1px solid;
        }
        td {
            text-align: left;
            border: 1px solid;
        }
    </style>
</head>
<body>
成功，云端的响应消息如下： <br/>
<div>
    <table>
        <tr>
            <th>RequestId</th>
            <td>${result.getString("requestId")}</td>
        </tr>
        <tr>
            <th>是否成功</th>
            <td>${result.getString("success")}</td>
        </tr>
        <tr>
            <th>错误信息</th>
            <td>${result.getString("error")}</td>
        </tr>
    </table>
</div>
</body>
</html>
