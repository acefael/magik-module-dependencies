
var acefael = acefael || {}

acefael.w = 960
acefael.h = 500

acefael.linked = {}
acefael.links.forEach(function(link){
  acefael.linked[link.source+'/'+link.target] = true })

acefael.svg = d3.select('body').append('svg:svg').attr({ width: acefael.w ,
                                                         height: acefael.h } )

acefael.charge = function(n) {
  var c = 0
  var isBase = n.name.valueOf() == 'base'
  for( var i = 0 ; i < acefael.links.length ; i++ ) {
    var l = acefael.links[i];
    if( l.target.name.valueOf() == n.name.valueOf() ) {
      ++c } }
  var rv = -( 30 + (c*10) )
  return rv }

acefael.force = d3.layout.force()
  .size([acefael.w,acefael.h])
  .nodes(acefael.nodes)
  .links(acefael.links)
  .charge( acefael.charge )

acefael.force.start()

acefael.link = acefael.svg
  .selectAll('line.link')
  .data(acefael.links)
  .enter()
  .append('svg:line')
  .attr('class','link')
  .style('stroke','#888')

acefael.node = acefael.svg
  .selectAll('g.node')
  .data(acefael.force.nodes())
  .enter()
  .append('svg:g')
  .attr('class','node')
  .append('svg:circle')
  .attr('r',5)
  .on('mouseover',function(d) {

    var elms;

    // hovered node green
    d3.select(this).attr('fill','green')
    // nodes nodes this depends on orange

    elms = acefael.node.select(
      function(n) {
        return acefael.linked[d.index+'/'+n.index] ? this : null })
    elms.attr('fill','orange')

    // nodes depending on this blue
    elms = acefael.node.select(
      function(n) {
        return acefael.linked[n.index+'/'+d.index] ? this : null })
    elms.attr('fill','blue') } )

  .on('mouseout',function() {
    acefael.node.attr('fill','black')})

acefael.title = acefael.node
  .append('title')
  .text(function(n){
    var r = n.name
    if( n.version != undefined ) {
      r = r + '.' + n.version }
    return r })

acefael.node.call(acefael.force.drag)

acefael.force.on('tick',function(){
  acefael.link.attr({x1: function(d){return d.source.x} ,
                     y1: function(d){return d.source.y} ,
                     x2: function(d){return d.target.x} ,
                     y2: function(d){return d.target.y}})
  acefael.node.attr({cx:function(d){return d.x} ,
                     cy:function(d){return d.y}})})
