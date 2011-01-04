$(document).ready(function(){
  // Persist the directory's visibility.
  if (getCookie('show_dir') == 'true') {
    $('#directory').show();
    $('#expand').html('&minus; Menu');
  }
  
  // Hook up the directory expander.
  $('#expand').click(function(event){
    $('#directory').animate({
      height: 'toggle'
    }, 300, function() {
      if ($('#directory').css('display') == 'none') {
        $('#expand').html('+ Menu');
        document.cookie = 'show_dir=null; path=/';
      } else {
        $('#expand').html('&minus; Menu');
        document.cookie = 'show_dir=true; path=/';
      }
    });

    event.preventDefault();
  });
});

function getCookie(name) {
  var result = document.cookie.match('(^|;) ?' + name + '=([^;]*)(;|$)');
  if (result) {
    return unescape(result[2]);
  } else {
    return null;
  }
}
