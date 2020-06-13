<%--
  Created by IntelliJ IDEA.
  User: lenovo
  Date: 2020/5/4
  Time: 13:12
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>云端设备管理页面</title>

    <script src="${pageContext.request.contextPath}/static/js/jquery-3.4.1.js"></script>
</head>
<body>
<div style="height: 100px">
    <span>
        <button onclick="update()">点击刷新</button>
        <p>
            设备当前温度：<span id="temp">未知</span><br/>
            设备状态：<span id="status">未知</span>
        </p>
    </span>
</div>
<div>
    <form id="setProperty">
        <fieldset>
            <legend>设置设备期望属性值</legend>
            <label>
                输入设备期望温度：
                <input type="text" name="temperature"/>
            </label>
            <input type="button" id="submit" value="设置">
        </fieldset>
    </form>
    <br/>
    <form action="${pageContext.request.contextPath}/invokeService">
        <fieldset>
            <legend>选择需要调用的设备服务</legend>
            请选择设备服务：
            <label content="">
                <select name="serviceName">
                    <option>SetWarningTemp</option>
                </select>
            </label>
            <br/>
            报警最高温度：
            <label>
                <input type="text" name="max"/>
            </label>
            <br/>
            报警最低温度：
            <label content="">
                <input type="text" name="min"/>
            </label>
            <br/>
            <input type="submit" value="调用">
        </fieldset>
    </form>
</div>

<script>
    window.onload = update;

    // 更新设备属性前，先判断设备是否在线，再决定请求哪个 url
    $("#submit").click(function () {
        var status = $("#status").html();
        var url;
        console.log(status);
        if (status === "ONLINE") {    // 如果设备在线，直接设置
            url = "/setProperty";
        } else {  // 如果设备离线或者未知，更新设备影子
            url = "/updateShadowTemp"
        }
        console.log(url);
        $.ajax({
            url: url,
            data: $("#setProperty").serialize(),
            async: false,
            success: function () {
                alert("成功更新设备属性");

                // 更新服务端设备状态显示
                update();
            },
            error: function () {
                alert("更新设备属性失败");
            }
        });
    });

    function getShadowTemp() {
        var temperature = document.getElementById("temp");
        $.ajax({
                url: "/getShadowTemp",
                type: "GET",
                data: {},
                dataType: "text",
                success: function (temp) {
                    if (temp > -100) {
                        temperature.innerHTML = temp;
                    } else {
                        temperature.innerHTML = "获取设备温度失败";
                    }
                },
                error: function () {
                    temperature.innerHTML = "获取设备温度失败";
                }
            }
        )
    }

    function updateDeviceStatus() {
        var temperature = document.getElementById("status");
        $.ajax({
                url: "/getStatus",
                type: "GET",
                data: {},
                dataType: "text",
                success: function (temp) {
                    console.log("成功获取设备状态");
                    temperature.innerHTML = temp;
                },
                error: function () {
                    console.log("获取设备状态失败了");
                    temperature.innerHTML = "获取设备状态失败";
                }
            }
        )
    }

    function update() {
        // 先更新设备状态，再决定后续操作步骤
        updateDeviceStatus();
        getShadowTemp();
    }
</script>
</body>
</html>
