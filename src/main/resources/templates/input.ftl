<html>
<head>


<link rel="stylesheet" href="/css/input.css" />
<script  src="https://js.stripe.com/v3/"></script>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" crossorigin="anonymous">

</head>
<body>
<div class="container">
 <div class="row justify-content-md-center mt-5">
      <div class = "col-md-6">
      <div class="card shadowLarge">
  <div class="card-body">
    <h5 class="card-title">Payment Form</h5>
<form action="/charge" method="post" id="payment-form">
  <div class="row">
  <div class="form-group">
      <input type="text" class="form-control" id="descriptor" name="descriptor" placeholder="Example Descriptor">
  </div>
  </div>
  <div class="row">
  <div class="form-group">
      <input type="text" class="form-control" id="amount" name="amount" placeholder="Example Amount">
  </div>
  </div>
  <div class="row">
    <div id="card-element" class="form-control">
      <!-- A Stripe Element will be inserted here. -->
    </div>
    </div>
    <!-- Used to display form errors. -->
    <div class="row" id="card-errors" role="alert">
    
  </div>
<div class="row pt-2">
  <button id ="card-button" >Submit Payment</button>
  </div>
</form>  
<script src="/js/input.js"></script>
</div>
  </div>
  </div>

  </div>
  </div>

  </div>

</body>
</html>