<ui:auth title="Log in">
    <div>
        <span class="eyebrow">Welcome back</span>
        <h1>Log in to your orchard</h1>
        <p class="muted" style="margin-top:6px">Enter your email and password to continue.</p>
    </div>
    <form method="post" action="${ctx}/login" class="grid" novalidate>
        <ui:csrf/>
        <ui:input name="email" label="Email address" type="email" value="${f.email}" required="true" autocomplete="email" placeholder="you@example.com"/>
        <ui:input name="password" label="Password" type="password" required="true" autocomplete="current-password"/>
        <button type="submit" class="btn btn-primary">Log in</button>
    </form>
    <p class="muted">Don't have an account? <a href="${ctx}/register">Create one</a></p>
    <div class="demo">
        Demo accounts (password <b>password</b> on local setups):<br>
        <b>admin@example.com</b> · <b>staff@example.com</b> · <b>viewer@example.com</b>
    </div>
</ui:auth>
