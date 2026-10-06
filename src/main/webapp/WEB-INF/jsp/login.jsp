<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>ログイン | うさぎ鉄道 電力・車両保守システム</title>
<link rel="stylesheet" href="${ctx}/static/css/urms.css">
<script src="${ctx}/static/js/urms-theme.js"></script>
</head>
<body class="login">
<div class="login-box">
  <div class="theme-row"><button type="button" id="theme-toggle" class="theme" title="表示テーマを切り替え" aria-pressed="false">&#9790; ダーク表示</button></div>
  <h1>うさぎ鉄道<br><small>電力・車両保守システム (URMS) 端末ログイン</small></h1>
  <c:if test="${param.error != null}"><div class="flash ng">ユーザIDまたはパスワードが正しくありません</div></c:if>
  <c:if test="${param.logout != null}"><div class="flash ok">ログアウトしました</div></c:if>
  <form action="${ctx}/login" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
    <label>ユーザID <input type="text" name="username" autofocus required></label>
    <label>パスワード <input type="password" name="password" required></label>
    <button type="submit" class="primary">ログイン</button>
  </form>
  <p class="hint">検証用: shirei / shirei123 (電力指令), kenshu / kenshu123 (検修),<br>maker / maker123 (メーカー), admin / admin123</p>
</div>
</body>
</html>
