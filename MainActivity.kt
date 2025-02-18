class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Set your adapter here

        // Setup FAB with ID add_Url
        val fab = findViewById<FloatingActionButton>(R.id.add_Url)
        fab.setOnClickListener {
            // Handle FAB click - Open URLActivity
            val intent = Intent(this, URLActivity::class.java)
            startActivity(intent)
        }
    }
} 