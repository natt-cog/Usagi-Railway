<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="故障登録" />
<%@ include file="../common/header.jspf" %>
<h1>故障登録</h1>
<div class="panel">
<form action="${ctx}/rolling/failures" method="post" class="failure">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <table class="kv">
    <tr><th>製造番号<span class="req">必須</span></th><td><input type="text" name="serialNo" class="serial" value="<c:out value='${serialNo}' />" size="14" maxlength="12" required>
      <span id="serial-lookup" class="note"></span></td></tr>
    <tr><th>発生日時</th><td><input type="text" name="occurredAt" size="16" placeholder="yyyy/MM/dd HH:mm"> <span class="note">未入力の場合は現在日時</span></td></tr>
    <tr><th>故障内容<span class="req">必須</span></th><td><textarea name="symptom" rows="3" maxlength="200" required></textarea></td></tr>
    <tr><th>故障コード</th><td><input type="text" name="failureCode" size="10" maxlength="8"> <span class="note">機器モニタ表示のコード (例: E-OC1)</span></td></tr>
    <tr><th>重要度<span class="req">必須</span></th><td>
      <c:forEach var="s" items="${severities}"><label><input type="radio" name="severity" value="${s}" ${s == 'B' ? 'checked' : ''}> ${s.label}</label> </c:forEach>
      <div class="note">重要度 A (運行支障) を登録すると、運用中の編成は自動的に休車になります。</div></td></tr>
  </table>
  <p><button type="submit" class="primary">登録</button> <a href="${ctx}/rolling/failures">戻る</a></p>
</form>
</div>
<%@ include file="../common/footer.jspf" %>
