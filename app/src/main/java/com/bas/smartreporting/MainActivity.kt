package com.bas.smartreporting

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val Navy = Color(0xFF102A43)
private val Blue = Color(0xFF1769AA)
private val Gold = Color(0xFFF0A52B)

data class Cat(val title:String, val subs:List<String> = emptyList())
data class Item(val id:Long,val cat:String,val sub:String,val department:String,val title:String,val note:String,val date:String,val media:String,val type:String)

private val cats = listOf(
    Cat("5R Safety Man"),Cat("Airwash"),Cat("APAR"),Cat("Briefing K3"),Cat("Denah Layout"),
    Cat("Foto Kegiatan"),Cat("Hydrant"),
    Cat("Kotak P3K",listOf("Boiler","MTN","Pos Security","BM & Finishing","Vehicle","Logistik")),
    Cat("Instruksi Kerja"),
    Cat("Limbah B3",listOf("Limbah Majun","Kemasan Bekas","Filter Bekas","Terkontaminasi","Resin","Aki","Minyak Pelumas")),
    Cat("P2K3"),Cat("Soal Semester"),Cat("Memo & Surat Jalan"),
    Cat("Temuan 5R",listOf("Armada","QA","Logistik BB","Logistik PJ","MTN","BM","Finishing","HRD & GA"))
)

class DB(c:Context):SQLiteOpenHelper(c,"bas.db",null,2) {
    override fun onCreate(d:SQLiteDatabase) {
        d.execSQL("CREATE TABLE reports(id INTEGER PRIMARY KEY AUTOINCREMENT,cat TEXT,sub TEXT,department TEXT NOT NULL DEFAULT '',title TEXT,note TEXT,date TEXT,media TEXT,type TEXT)")
        d.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY,v TEXT)")
        d.execSQL("INSERT INTO settings VALUES('pin','123456')")
    }
    override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int) {
        if(o<2) d.execSQL("ALTER TABLE reports ADD COLUMN department TEXT NOT NULL DEFAULT ''")
    }
    fun add(cat:String,sub:String,department:String,title:String,note:String,date:String,media:String,type:String) {
        val v=ContentValues().apply { put("cat",cat);put("sub",sub);put("department",department);put("title",title);put("note",note);put("date",date);put("media",media);put("type",type) }
        writableDatabase.insert("reports",null,v)
    }
    fun update(id:Long,cat:String,sub:String,department:String,title:String,note:String,date:String,media:String,type:String) {
        val v=ContentValues().apply { put("cat",cat);put("sub",sub);put("department",department);put("title",title);put("note",note);put("date",date);put("media",media);put("type",type) }
        writableDatabase.update("reports",v,"id=?",arrayOf(id.toString()))
    }
    fun delete(id:Long) { writableDatabase.delete("reports","id=?",arrayOf(id.toString())) }
    fun all():List<Item> {
        val a=mutableListOf<Item>()
        readableDatabase.rawQuery("SELECT id,cat,sub,department,title,note,date,media,type FROM reports ORDER BY id DESC",null).use { c ->
            while(c.moveToNext()) a.add(Item(c.getLong(0),c.getString(1),c.getString(2)?: "",c.getString(3)?: "",c.getString(4)?: "",c.getString(5)?: "",c.getString(6),c.getString(7)?: "",c.getString(8)?: ""))
        }
        return a
    }
    fun pin()=readableDatabase.rawQuery("SELECT v FROM settings WHERE k='pin'",null).use { if(it.moveToFirst()) it.getString(0) else "123456" }
    fun setPin(p:String) { writableDatabase.execSQL("UPDATE settings SET v=? WHERE k='pin'",arrayOf(p)) }
}

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); setContent { BASApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BASApp() {
    val c= LocalContext.current
    val db= remember { DB(c) }
    var logged by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Dashboard") }
    var category by remember { mutableStateOf(cats[0]) }
    var sub by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var media by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var reportPeriod by remember { mutableStateOf("Semua") }
    var reportQuery by remember { mutableStateOf("") }
    var departmentFilter by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<Item?>(null) }
    var status by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    val data= remember(reload) { db.all() }
    val reportData = remember(data, reportPeriod) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val month = today.substring(0, 7)
        val periodData = when(reportPeriod) {
            "Harian" -> data.filter { it.date.startsWith(today) }
            "Bulanan" -> data.filter { it.date.startsWith(month) }
            else -> data
        }
        periodData.filter {
            (reportQuery.isBlank() || listOf(it.title,it.note,it.cat,it.sub,it.department).any { value -> value.contains(reportQuery.trim(),ignoreCase=true) }) &&
            (departmentFilter.isBlank() || it.department.contains(departmentFilter.trim(),ignoreCase=true))
        }
    }

    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b ->
        if(b!=null) {
            val dir=File(c.filesDir,"media").apply{mkdirs()}
            val f=File(dir,"IMG_${System.currentTimeMillis()}.jpg")
            f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG,90,it) }
            media=f.absolutePath;mediaType="FOTO";status="Foto tersimpan"
        }
    }
    val video=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if(r.resultCode==Activity.RESULT_OK) {
            val u=r.data?.data
            if(u!=null) {
                val f=File(File(c.filesDir,"media").apply{mkdirs()},"VID_${System.currentTimeMillis()}.mp4")
                c.contentResolver.openInputStream(u)?.use { input -> f.outputStream().use { input.copyTo(it) } }
                media=f.absolutePath;mediaType="VIDEO";status="Video tersimpan"
            }
        }
    }
    val backup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { u ->
        if(u!=null) try { backup(c,u);status="Backup berhasil" } catch(e:Exception) { status="Backup gagal: ${e.message}" }
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if(u!=null) try { db.close();restore(c,u);reload++;status="Restore berhasil. Buka ulang aplikasi." } catch(e:Exception) { status="Restore gagal: ${e.message}" }
    }
    val pdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { u -> if(u!=null) { exportPdf(c,u,reportData);status="PDF selesai" } }
    val jpg=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { u -> if(u!=null) { exportJpg(c,u,reportData);status="JPG selesai" } }
    val xlsx=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { u -> if(u!=null) { exportXlsx(c,u,reportData);status="Excel XLSX selesai" } }

    MaterialTheme(colorScheme=lightColorScheme(primary=Blue,secondary=Gold)) {
        if(!logged) {
            Column(Modifier.fillMaxSize().background(Navy).padding(24.dp),verticalArrangement=Arrangement.Center) {
                Text("BAS SMART REPORTING",color=Gold,fontSize=27.sp,fontWeight=FontWeight.Black)
                Text("PT. Bangunperkasa Adhitamasentra",color=Color.White)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(pin,{pin=it},label={Text("PIN Admin")},visualTransformation=PasswordVisualTransformation())
                Button(onClick={if(pin==db.pin()) logged=true else status="PIN salah"}) { Text("MASUK") }
                Text("PIN awal: 123456 (segera ganti)",color=Color.LightGray,fontSize=12.sp)
                Spacer(Modifier.height(38.dp))
                Text("Dikembangkan oleh Deni",color=Color.LightGray,fontSize=12.sp)
                Text(status,color=Gold)
            }
        } else {
            Scaffold(topBar={ TopAppBar(title={Column { Text("BAS SMART REPORTING",fontWeight=FontWeight.Bold);Text("PT. Bangunperkasa Adhitamasentra",fontSize=11.sp) }}) },
                bottomBar={ NavigationBar { listOf("Dashboard","Input","Laporan","Backup","Admin").forEach { t ->
                    NavigationBarItem(selected=tab==t,onClick={tab=t},icon={},label={Text(t)})
                } } }) { p ->
                Column(Modifier.fillMaxSize().padding(p).padding(14.dp)) {
                    when(tab) {
                        "Dashboard" -> {
                            Text("Dashboard Kegiatan",fontSize=22.sp,fontWeight=FontWeight.Bold)
                            Text("Total laporan: ${data.size}     Dokumentasi: ${data.count{it.media.isNotBlank()}}")
                            Spacer(Modifier.height(12.dp))
                            LazyColumn { items(cats) { x ->
                                Card(Modifier.fillMaxWidth().padding(vertical=3.dp),onClick={category=x;sub="";tab="Input"}) {
                                    Text("${x.title}     (${data.count{it.cat==x.title}})",Modifier.padding(14.dp))
                                }
                            } }
                        }
                        "Input" -> {
                            Text("Input Laporan",fontSize=22.sp,fontWeight=FontWeight.Bold)
                            var open by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded=open,onExpandedChange={open=!open}) {
                                OutlinedTextField(category.title,{},readOnly=true,label={Text("Kategori")},modifier=Modifier.fillMaxWidth().menuAnchor())
                                ExposedDropdownMenu(expanded=open,onDismissRequest={open=false}) { cats.forEach { x -> DropdownMenuItem(text={Text(x.title)},onClick={category=x;sub="";open=false}) } }
                            }
                            if(category.subs.isNotEmpty()) {
                                var so by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded=so,onExpandedChange={so=!so}) {
                                    OutlinedTextField(sub.ifBlank{"Pilih subfolder"},{},readOnly=true,label={Text("Subfolder")},modifier=Modifier.fillMaxWidth().menuAnchor())
                                    ExposedDropdownMenu(expanded=so,onDismissRequest={so=false}) { category.subs.forEach { s -> DropdownMenuItem(text={Text(s)},onClick={sub=s;so=false}) } }
                                }
                            }
                            OutlinedTextField(department,{department=it},label={Text("Bagian / Departemen (wajib)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                            OutlinedTextField(title,{title=it},label={Text("Judul kegiatan")},modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(note,{note=it},label={Text("Keterangan")},modifier=Modifier.fillMaxWidth().heightIn(min=90.dp))
                            Text("Dokumentasi: ${mediaType.ifBlank{"Belum ada"}}")
                            Row { OutlinedButton(onClick={camera.launch(null)}) { Text("Ambil Foto") };Spacer(Modifier.width(8.dp));OutlinedButton(onClick={video.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))}) { Text("Rekam Video") } }
                            Button(onClick={
                                if(department.isBlank()) status="Bagian/departemen wajib diisi"
                                else if(title.isBlank()) status="Judul wajib diisi"
                                else if(category.subs.isNotEmpty()&&sub.isBlank()) status="Pilih subfolder"
                                else {
                                    val d=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(Date())
                                    val targetId = editingId
                                    if(targetId == null) db.add(category.title,sub,department.trim(),title,note,d,media,mediaType)
                                    else db.update(targetId,category.title,sub,department.trim(),title,note,d,media,mediaType)
                                    editingId=null;department="";title="";note="";sub="";media="";mediaType="";reload++
                                    status=if(targetId==null) "Laporan tersimpan" else "Perubahan laporan tersimpan"
                                }
                            },modifier=Modifier.fillMaxWidth()) { Text(if(editingId==null) "SIMPAN LAPORAN" else "SIMPAN PERUBAHAN") }
                            if(editingId!=null) OutlinedButton(onClick={editingId=null;department="";title="";note="";sub="";media="";mediaType="";status="Edit dibatalkan"},modifier=Modifier.fillMaxWidth()) { Text("BATAL EDIT") }
                        }
                        "Laporan" -> {
                            Text("Ekspor Laporan",fontSize=22.sp,fontWeight=FontWeight.Bold)
                            Text("Data tersedia: ${reportData.size} laporan")
                            OutlinedTextField(reportQuery,{reportQuery=it},label={Text("Cari judul, keterangan, kategori")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                            OutlinedTextField(departmentFilter,{departmentFilter=it},label={Text("Filter bagian/departemen")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                            var periodOpen by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded=periodOpen,onExpandedChange={periodOpen=!periodOpen}) {
                                OutlinedTextField(reportPeriod,{},readOnly=true,label={Text("Periode laporan")},modifier=Modifier.fillMaxWidth().menuAnchor())
                                ExposedDropdownMenu(expanded=periodOpen,onDismissRequest={periodOpen=false}) {
                                    listOf("Semua","Harian","Bulanan").forEach { p -> DropdownMenuItem(text={Text(p)},onClick={reportPeriod=p;periodOpen=false}) }
                                }
                            }
                            Button(onClick={pdf.launch("BAS_Laporan_${reportPeriod}.pdf") },modifier=Modifier.fillMaxWidth()) { Text("Download PDF") }
                            Button(onClick={jpg.launch("BAS_Ringkasan_${reportPeriod}.jpg")},modifier=Modifier.fillMaxWidth()) { Text("Download JPG") }
                            Button(onClick={xlsx.launch("BAS_Data_${reportPeriod}.xlsx")},modifier=Modifier.fillMaxWidth()) { Text("Download Excel XLSX") }
                            LazyColumn { items(reportData) { r ->
                                Card(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(r.title,fontWeight=FontWeight.Bold)
                                        Text("${r.date} • ${r.department} • ${r.cat}${if(r.sub.isNotBlank()) " / ${r.sub}" else ""}",fontSize=12.sp)
                                        if(r.note.isNotBlank()) Text(r.note,fontSize=12.sp)
                                        Text("${r.type.ifBlank { "Tanpa dokumentasi" }}${if(r.media.isNotBlank()) " • ${File(r.media).name}" else ""}",fontSize=11.sp,color=Color.Gray)
                                        Row {
                                            TextButton(onClick={
                                                editingId=r.id
                                                category=cats.firstOrNull{it.title==r.cat}?:cats[0]
                                                sub=r.sub;department=r.department;title=r.title;note=r.note;media=r.media;mediaType=r.type
                                                tab="Input";status="Mode edit laporan #${r.id}"
                                            }) { Text("EDIT") }
                                            TextButton(onClick={pendingDelete=r}) { Text("HAPUS",color=MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            } }
                        }
                        "Backup" -> {
                            Text("Backup Database & Media",fontSize=22.sp,fontWeight=FontWeight.Bold)
                            Text("Simpan ZIP ke lokasi di luar aplikasi untuk melindungi data ketika aplikasi dihapus.")
                            Button(onClick={backup.launch("BAS_Backup.zip")},modifier=Modifier.fillMaxWidth()) { Text("BUAT BACKUP ZIP") }
                            OutlinedButton(onClick={restore.launch(arrayOf("application/zip","*/*"))},modifier=Modifier.fillMaxWidth()) { Text("RESTORE BACKUP ZIP") }
                        }
                        "Admin" -> {
                            Text("Pengaturan Admin",fontSize=22.sp,fontWeight=FontWeight.Bold)
                            OutlinedTextField(newPin,{newPin=it},label={Text("PIN baru (minimal 6 angka)")},visualTransformation=PasswordVisualTransformation())
                            Button(onClick={if(newPin.length>=6&&newPin.all{it.isDigit()}) { db.setPin(newPin);newPin="";status="PIN diubah" } else status="PIN harus minimal 6 digit angka" }) { Text("SIMPAN PIN") }
                            Spacer(Modifier.weight(1f))
                            Text("Dikembangkan oleh Deni",color=Color.Gray,fontSize=12.sp)
                        }
                    }
                    if(status.isNotBlank()) Text(status,color=Blue,fontSize=12.sp)
                    Text("Dikembangkan oleh Deni",color=Color.Gray,fontSize=10.sp)
                }
            }
        }
        if(pendingDelete!=null) {
            AlertDialog(
                onDismissRequest={pendingDelete=null},
                title={Text("Hapus laporan?")},
                text={Text("Laporan '${pendingDelete?.title}' akan dihapus dari database. File media tidak otomatis dihapus." )},
                confirmButton={TextButton(onClick={pendingDelete?.let{db.delete(it.id);reload++;status="Laporan dihapus"};pendingDelete=null}){Text("HAPUS",color=MaterialTheme.colorScheme.error)}},
                dismissButton={TextButton(onClick={pendingDelete=null}){Text("BATAL")}}
            )
        }
    }
}

private fun backup(c:Context,u:Uri) {
    c.contentResolver.openOutputStream(u)!!.use { o -> ZipOutputStream(o).use { z ->
        val db=c.getDatabasePath("bas.db"); if(db.exists()) zipFile(z,db,"bas.db")
        File(c.filesDir,"media").takeIf{it.exists()}?.walkTopDown()?.filter{it.isFile}?.forEach{zipFile(z,it,"media/${it.name}")}
    } }
}
private fun zipFile(z:ZipOutputStream,f:File,n:String) { z.putNextEntry(ZipEntry(n));f.inputStream().use{it.copyTo(z)};z.closeEntry() }
private fun restore(c:Context,u:Uri) {
    val t=File(c.cacheDir,"restore").apply{deleteRecursively();mkdirs()}
    c.contentResolver.openInputStream(u)!!.use { input -> ZipInputStream(input).use { z ->
        var e=z.nextEntry
        while(e!=null) { val f=File(t,e.name);val root=t.canonicalFile.path+File.separator;val target=f.canonicalFile.path;require(target==t.canonicalFile.path||target.startsWith(root));if(!e.isDirectory){f.parentFile?.mkdirs();f.outputStream().use{z.copyTo(it)}};z.closeEntry();e=z.nextEntry }
    } }
    val db=File(t,"bas.db");require(db.exists());db.copyTo(c.getDatabasePath("bas.db"),true)
    val m=File(t,"media");if(m.exists()){val dest=File(c.filesDir,"media");dest.deleteRecursively();m.copyRecursively(dest,true)}
    t.deleteRecursively()
}
private fun exportPdf(c:Context,u:Uri,a:List<Item>) {
    val document=PdfDocument()
    val titlePaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=15f; isFakeBoldText=true; color=android.graphics.Color.rgb(16,42,67) }
    val bodyPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=9.5f; color=android.graphics.Color.DKGRAY }
    val linePaint=Paint().apply { color=android.graphics.Color.LTGRAY; strokeWidth=0.7f }
    val pageWidth=595; val pageHeight=842; val left=35f; val right=560f
    val rows=if(a.isEmpty()) listOf<Item>() else a
    val perPage=42
    val pageCount=maxOf(1,(rows.size+perPage-1)/perPage)
    for(pageNo in 0 until pageCount) {
        val page=document.startPage(PdfDocument.PageInfo.Builder(pageWidth,pageHeight,pageNo+1).create())
        val canvas=page.canvas
        canvas.drawText("BAS SMART REPORTING",left,38f,titlePaint)
        canvas.drawText("PT. Bangunperkasa Adhitamasentra",left,56f,bodyPaint)
        canvas.drawText("Laporan • ${a.size} data • Halaman ${pageNo+1}/$pageCount",left,76f,bodyPaint)
        canvas.drawLine(left,88f,right,88f,linePaint)
        var y=108f
        val start=pageNo*perPage
        val end=minOf(start+perPage,rows.size)
        if(rows.isEmpty()) canvas.drawText("Belum ada data laporan pada periode ini.",left,y,bodyPaint)
        else for(index in start until end) {
            val item=rows[index]
            val headline="${item.date} | ${item.department} | ${item.cat}${if(item.sub.isNotBlank()) " / ${item.sub}" else ""}"
            canvas.drawText(headline.take(105),left,y,bodyPaint); y+=13f
            canvas.drawText("${index+1}. ${item.title}".take(105),left+8f,y,bodyPaint); y+=12f
            if(item.note.isNotBlank()) {
                val note=item.note.replace('\n',' ').take(150)
                canvas.drawText("Keterangan: $note",left+8f,y,bodyPaint); y+=12f
            }
            if(item.media.isNotBlank()) { canvas.drawText("Dokumentasi: ${item.type} • ${File(item.media).name}".take(100),left+8f,y,bodyPaint); y+=12f }
            y+=5f
            canvas.drawLine(left,y,right,y,linePaint);y+=9f
        }
        document.finishPage(page)
    }
    c.contentResolver.openOutputStream(u)!!.use{document.writeTo(it)}
    document.close()
}
private fun exportJpg(c:Context,u:Uri,a:List<Item>) {
    val b=Bitmap.createBitmap(1200,1600,Bitmap.Config.ARGB_8888);val cv=Canvas(b);cv.drawColor(android.graphics.Color.WHITE)
    val p=Paint().apply{color=android.graphics.Color.rgb(16,42,67);textSize=32f}
    cv.drawText("BAS SMART REPORTING",45f,75f,p);p.textSize=22f;cv.drawText("PT. Bangunperkasa Adhitamasentra",45f,120f,p)
    cv.drawText("Jumlah laporan: ${a.size}",45f,190f,p);var y=250f
    a.take(45).forEach{cv.drawText("${it.date.take(10)} | ${it.department} | ${it.cat} | ${it.title.take(25)}",45f,y,p);y+=28}
    c.contentResolver.openOutputStream(u)!!.use{b.compress(Bitmap.CompressFormat.JPEG,90,it)};b.recycle()
}
private fun esc(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
private fun exportXlsx(c:Context,u:Uri,a:List<Item>) {
    val rows=mutableListOf(listOf("ID","Tanggal","Departemen","Kategori","Subfolder","Judul","Keterangan","Media","Lokasi Media"))
    a.forEach{rows.add(listOf(it.id.toString(),it.date,it.department,it.cat,it.sub,it.title,it.note,it.type,it.media))}
    c.contentResolver.openOutputStream(u)!!.use { out -> ZipOutputStream(out).use { z ->
        fun put(n:String,s:String){z.putNextEntry(ZipEntry(n));z.write(s.toByteArray());z.closeEntry()}
        put("[Content_Types].xml","""<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
        put("_rels/.rels","""<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
        put("xl/workbook.xml","""<?xml version="1.0"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Laporan BAS" sheetId="1" r:id="rId1"/></sheets></workbook>""")
        put("xl/_rels/workbook.xml.rels","""<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
        val sb=StringBuilder("""<?xml version="1.0"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed{ri,row->sb.append("<row r=\"${ri+1}\">");row.forEachIndexed{ci,v->
            var n=ci+1;var col="";while(n>0){val r=(n-1)%26;col=(65+r).toChar()+col;n=(n-1)/26}
            sb.append("<c r=\"$col${ri+1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>")
        };sb.append("</row>")}
        sb.append("</sheetData></worksheet>");put("xl/worksheets/sheet1.xml",sb.toString())
    } }
}
