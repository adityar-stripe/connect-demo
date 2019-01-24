<html>
<head>
<link rel="stylesheet" href="/css/input.css" />
</head>
<body>
<button id ="card-button"
 onclick="window.location.href='https://connect.stripe.com/oauth/authorize?response_type=code&client_id=${platform_id}&scope=read_write'" >
 Connect with stripe</button>
</body>
</html>