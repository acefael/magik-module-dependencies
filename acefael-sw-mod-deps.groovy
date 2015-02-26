
class SmallworldModuleDependencies implements Runnable {
  public static void main( String[] args ) {
    def t = new Thread(new SmallworldModuleDependencies( args: args ))
    t.start()
    t.join() }

  String[] args;

  public void run() {

    // herein will will store all module definitions
      def allModules = []

    // predicate to determine if a file is a module.def file
    def moduleDef = { f ->
      def fn = f.name
      f.file && fn.toLowerCase().equals( 'module.def' ) }

    // loop method
    def eachModuleDef = { f , cl ->
      f.eachFileRecurse { r ->
        if( moduleDef(r) ) { cl.call(r) } } }

    // slurp content lines from module def file
    def lines = { f ->
      def lines = f.readLines()
      lines = lines.collect { it.replaceAll('#.*','') }
      lines = lines.findAll { it.trim().size() > 0 }
      lines }

    // parse module def file
    def processModuleDef = { base , f ->
      def l = lines( f )
      def name = l.head().split('\\s+')
      def version = null
      if( name.length > 1 ) { version = name[1] }
      name = name[0]

      def module = [:]

      module.file = f.absolutePath.substring(base.absolutePath.size()+1)
      module.name = name
      module.version = version

      def currentSection = null
      def currentSectionContents = []

      l.tail().each { line ->
        // not interested in language or hidden
        if( line =~ /^language\s+/ ) { return }
        if( line =~ /^hidden\s*/ ) { return }
        // todo could pay attention only to sections interested in
        // not starting with blank?
        if( line =~ '^\\S' ) {
          // no current section? then the line starts one
          if( null == currentSection ) {
            currentSection = line.trim()
            return }
          // ending a section?
          if( line.trim().toLowerCase().equals('end') ) {
            module[ currentSection ] = currentSectionContents//.collect { [ it.trim().split('\\s+') , null][0..1] }
            currentSection = null
            currentSectionContents = []
            return } }
        // otherwise just add to current section
        currentSectionContents.add( line ) }

      if( module.description ) {
        module.description = module.description.collect{it.trim()}.join(' ') }
      if( module.requires ) {
        module.requires =
          module.requires.collect{[it.trim().split('\\s+'),null].flatten()[0..1]}}
      allModules.add( module ) }

    try {

      args.each { filename ->
        def f = new File(filename)
        eachModuleDef( f , processModuleDef.curry(f) ) }

      // need index of the modules for d3
      allModules.eachWithIndex { m , i -> m.index = i }

      println('var acefael = acefael || {}')
      print('acefael.nodes= ')
      println(groovy.json.JsonOutput.toJson(allModules))

      def links = []

      allModules.each { module ->
        module.requires.each { require ->
          def candidates = allModules.findAll { it.name.equalsIgnoreCase(require[0]) }
          if( candidates.empty ) {
            System.err.println("unable to resolve dependency $require of module ${module.file}")
            return }
          if( candidates.size() == 1 ) {
            links.add( [source: module.index, target: candidates.head().index ] )
            return
          }
          if( null != require[1] ) {
            def c2 = candidates.findAll {it.version.equals(require[1])}
            if( false == c2.empty ) {
              c2.each { c ->
                links.add( [source:module.index,target:c.index] ) }
              return } }
          // 'tis here when either require did not have a version or
          // module is found with different versions
          //
          // try to pick highest versioned module
          def c = candidates.sort { a,b ->
            ( a.version ?: '0' ).compareTo( b.version?:'0') }.last()
          links.add( [source:module.index,target:c.index] ) } }

      print('acefael.links=')
      println(groovy.json.JsonOutput.toJson(links))

    }
    catch(Exception e){e.printStackTrace(System.err)}}}
