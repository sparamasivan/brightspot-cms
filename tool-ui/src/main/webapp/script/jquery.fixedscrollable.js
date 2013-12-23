(function($, window, undefined) {

var $window = $(window);

function resizeScrollable() {
    var $scrollable = $(this),
            $popup = $scrollable.closest('.popup'),
            scrollableTop,
            bottomPadding;

    $scrollable.css('max-height', '');

    scrollableTop = $scrollable.offset().top;
    bottomPadding =
            $popup.outerHeight(true) -
            $scrollable.outerHeight(true) -
            scrollableTop +
            $popup.offset().top;

    $scrollable.css('max-height',
            $window.height() -
            scrollableTop +
            $window.scrollTop() -
            bottomPadding -
            20);
}

$.plugin2('fixedScrollable', {
    '_init': function(selector) {
        var plugin = this;

        plugin.$caller.on('mousewheel', selector, function(event, delta, deltaX, deltaY) {
            var $list = $(this),
                    maxScrollTop = $.data(this, 'dropDown-maxScrollTop');

            if (typeof maxScrollTop === 'undefined') {
                maxScrollTop = $list.prop('scrollHeight') - $list.innerHeight();
                $.data(this, 'dropDown-maxScrollTop', maxScrollTop);
            }

            if ((deltaY > 0 && $list.scrollTop() === 0) ||
                    (deltaY < 0 && $list.scrollTop() >= maxScrollTop)) {
                event.preventDefault();
            }
        });

        $window.resize($.throttle(100, function() {
            plugin.$caller.find(selector).each(resizeScrollable);
        }));
    },

    '_create': function(scrollable) {
        var $scrollable = $(scrollable);

        if ($scrollable.closest('.popup').css('position') === 'fixed') {
            resizeScrollable.call($scrollable[0]);
        }
    }
});

})(jQuery, window);
