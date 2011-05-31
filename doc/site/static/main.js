$(function() {
    $('h1, h2, h3, h4, h5').hover(
        function() {
            if (!this.id) return;
            this.innerHTML = '<a href="#' + this.id + '">' + this.innerText + '</a>';
        },
        function() {
            if (!this.id) return;
            this.innerText = $('a', this).text();
        });
});
