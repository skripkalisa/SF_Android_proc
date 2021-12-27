package com.example.filmsearch.view.fragments

import android.os.Bundle
import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmsearch.view.MainActivity
import com.example.filmsearch.R
import com.example.filmsearch.databinding.FragmentHomeBinding
import com.example.filmsearch.data.entity.Film
import com.example.filmsearch.utils.AnimationHelper
import com.example.filmsearch.view.rv_adapters.FilmListRecyclerAdapter
import com.example.filmsearch.view.rv_adapters.TopSpacingItemDecoration
import com.example.filmsearch.viewmodel.HomeFragmentViewModel
import java.util.*


class HomeFragment : Fragment() {

    private lateinit var filmsAdapter: FilmListRecyclerAdapter
    private lateinit var binding: FragmentHomeBinding
    private val viewModel by lazy {
        ViewModelProvider.NewInstanceFactory().create(HomeFragmentViewModel::class.java)
    }
    private var filmsDataBase = listOf<Film>()
        //Используем backing field
        set(value) {
            //Если придет такое же значение, то мы выходим из метода
            if (field == value) return
            //Если пришло другое значение, то кладем его в переменную
            field = value
            //Обновляем RV адаптер
            filmsAdapter.addItems(field)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnimationHelper.performFragmentCircularRevealAnimation(
            binding.homeFragmentRoot,
            requireActivity(),
            1
        )

        //Кладем нашу БД в RV
        viewModel.filmsListLiveData.observe(viewLifecycleOwner, {
            filmsDataBase = it
            filmsAdapter.addItems(it)
        })
//        filmsAdapter.addItems(filmsDataBase)
        sceneTransition(binding.homeFragmentRoot)

        initSearchView(view)

        initRecycler(view)
        initPullToRefresh()
    }

private fun initPullToRefresh() {
   //Вешаем слушатель, чтобы вызвался pull to refresh
   binding.pullToRefresh.setOnRefreshListener {
       //Чистим адаптер(items нужно будет сделать паблик или создать для этого публичный метод)
       filmsAdapter.items.clear()
       //Делаем новый запрос фильмов на сервер
       viewModel.getFilms()
       //Убираем крутящееся колечко
       binding.pullToRefresh.isRefreshing = false
   }
}


    private fun initRecycler(view: View) {
        val mainRecycler = view.findViewById<RecyclerView>(R.id.main_recycler)
        //находим наш RV
        mainRecycler.apply {
            filmsAdapter =
                FilmListRecyclerAdapter(object : FilmListRecyclerAdapter.OnItemClickListener {
                    override fun click(film: Film) {
                        (requireActivity() as MainActivity).launchDetailsFragment(film)
                    }
                })
            //Присваиваем адаптер
            adapter = filmsAdapter
            //Присвои layoutmanager
            layoutManager = LinearLayoutManager(requireContext())
            //Применяем декоратор для отступов
            val decorator = TopSpacingItemDecoration(8)
            addItemDecoration(decorator)
        }
    }

    private fun initSearchView(view: View) {
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        searchView.setOnClickListener {
            searchView.isIconified = false
        }

        //Подключаем слушателя изменений введенного текста в поиска
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            //Этот метод отрабатывает при нажатии кнопки "поиск" на софт клавиатуре
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            //Этот метод отрабатывает на каждое изменения текста
            override fun onQueryTextChange(newText: String): Boolean {
                //Если ввод пуст то вставляем в адаптер всю БД
                if (newText.isEmpty()) {
                    filmsAdapter.addItems(filmsDataBase)
                    return true
                }
                //Фильтруем список на поискк подходящих сочетаний
                val result = filmsDataBase.filter {
                    //Чтобы все работало правильно, нужно и запрос, и имя фильма приводить к нижнему регистру
                    it.title.lowercase(Locale.getDefault())
                        .contains(newText.lowercase(Locale.getDefault()))
                }
                //Добавляем в адаптер
                filmsAdapter.addItems(result)
                return true
            }
        })
    }

    private fun sceneTransition(homeFragmentRoot: ConstraintLayout?) {
        val scene = Scene.getSceneForLayout(
            homeFragmentRoot,
            R.layout.merge_home_screen_content,
            requireContext()
        )

        //Создаем анимацию выезда поля поиска сверху
        val searchSlide = Slide(Gravity.TOP).addTarget(R.id.search_view)
        //Создаем анимацию выезда RV снизу
        val recyclerSlide = Slide(Gravity.BOTTOM).addTarget(R.id.main_recycler)
        //Создаем экземпляр TransitionSet, который объединит все наши анимации
        val customTransition = TransitionSet().apply {
            //Устанавливаем время, за которое будет проходить анимация
            duration = 500
            //Добавляем сами анимации
            addTransition(recyclerSlide)
            addTransition(searchSlide)
        }
        //Также запускаем через TransitionManager, но вторым параметром передаем нашу кастомную анимацию
        TransitionManager.go(scene, customTransition)
    }
}